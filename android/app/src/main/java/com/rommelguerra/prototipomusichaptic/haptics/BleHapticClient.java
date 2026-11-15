package com.rommelguerra.prototipomusichaptic.haptics;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.SystemClock;
import java.util.Collections;

/** All GATT state belongs to main. Exactly one write awaiting both GATT and firmware ACK.
 * A single latest pending command prevents old microphone frames accumulating.
 */
@SuppressLint("MissingPermission")
public final class BleHapticClient {
    public interface Listener {
        void onState(String message, boolean ready);
        void onDevice(BluetoothDevice device, String name);
        void onAck(HapticProtocol.Ack ack);
    }
    private final Context context;
    private final Listener listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic commandCharacteristic;
    private boolean scanning, ready, verifying, centerAvailable, musicalAvailable, levelControlAvailable;
    private int protocolVersion=1;
    private int sequence;
    private final HapticCommandQueue queue=new HapticCommandQueue();
    private Flight flight;
    private static final class Flight {
        final int sequence; final byte[] payload;
        boolean written,acknowledged;
        Flight(int sequence,byte[] payload) { this.sequence=sequence; this.payload=payload; }
    }
    public BleHapticClient(Context context,Listener listener) {
        this.context=context.getApplicationContext(); this.listener=listener;
        BluetoothManager manager=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter=manager==null ? null : manager.getAdapter();
    }
    public boolean isLevelControlAvailable() { return ready && levelControlAvailable; }
    public boolean isMusicalAvailable() { return ready && musicalAvailable; }
    public boolean isCenterAvailable() { return ready && centerAvailable; }
    public int getProtocolVersion() { return protocolVersion; }
    public boolean isReady() { return ready; }
    public boolean isScanning() { return scanning; }
    public boolean isConnecting() { return gatt != null && !ready; }
    public void cancelScan() { if(!scanning) return; stopScan(); state(ready ? "Brazalete conectado y listo" : "Búsqueda detenida"); }
    private final Runnable scanTimeout=() -> { stopScan(); state("Búsqueda terminada. Elige tu ESP32; si no aparece, revisa el firmware."); };
    private final Runnable connectTimeout=() -> fail("Tiempo de conexión agotado. Revisa el firmware y vuelve a conectar.");
    private final Runnable writeTimeout=() -> fail("Sin confirmación del ESP32. Se desconecta y se detienen los motores.");
    private void state(String text) { listener.onState(text,ready); }
    public void scan() {
        disconnect();
        try {
            if (adapter==null || !adapter.isEnabled()) { state("Activa Bluetooth y pulsa Buscar ESP32."); return; }
            scanner=adapter.getBluetoothLeScanner();
            if (scanner==null) { state("Bluetooth LE no está disponible."); return; }
            scanning=true;
            ScanFilter filter=new ScanFilter.Builder().setServiceUuid(new ParcelUuid(HapticProtocol.SERVICE)).build();
            scanner.startScan(Collections.singletonList(filter),new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),scanCallback);
            state("Buscando durante 15 segundos…"); main.postDelayed(scanTimeout,15000);
        } catch (RuntimeException e) { fail("No se puede buscar: "+e.getMessage()); }
    }
    private final ScanCallback scanCallback=new ScanCallback() {
        @Override public void onScanResult(int type,ScanResult result) {
            main.post(() -> {
                if (!scanning) return;
                try {
                    String name=result.getScanRecord()==null ? null : result.getScanRecord().getDeviceName();
                    listener.onDevice(result.getDevice(),name==null ? "ESP32 Vibrafil" : name);
                } catch (SecurityException e) { fail("Falta permiso de dispositivos cercanos."); }
            });
        }
        @Override public void onScanFailed(int code) { main.post(() -> { if (scanning) fail("Error de búsqueda BLE: "+code); }); }
    };
    private void stopScan() {
        main.removeCallbacks(scanTimeout);
        if (scanning && scanner!=null) try { scanner.stopScan(scanCallback); } catch (RuntimeException ignored) {}
        scanning=false;
    }
    public void connect(BluetoothDevice device) {
        disconnect(); state("Conectando y comprobando el servicio háptico…");
        try {
            gatt=device.connectGatt(context,false,callbacks,BluetoothDevice.TRANSPORT_LE);
            if (gatt==null) { fail("Android no pudo crear la conexión."); return; }
            state("Conectando y comprobando el servicio háptico…");
            main.postDelayed(connectTimeout,15000);
        } catch (RuntimeException e) { fail("No se pudo conectar: "+e.getMessage()); }
    }
    private final BluetoothGattCallback callbacks=new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g,int status,int newState) {
            main.post(() -> {
                if (g!=gatt) { g.close(); return; }
                if (status!=BluetoothGatt.GATT_SUCCESS || newState==BluetoothProfile.STATE_DISCONNECTED) {
                    fail("Desconectado (GATT "+status+")."); return;
                }
                if (newState==BluetoothProfile.STATE_CONNECTED) {
                    try { if (!g.discoverServices()) fail("No se pudo iniciar el descubrimiento de servicios."); }
                    catch (RuntimeException e) { fail("Error de permisos Bluetooth."); }
                }
            });
        }
        @Override public void onServicesDiscovered(BluetoothGatt g,int status) {
            main.post(() -> {
                if (g!=gatt) return;
                if (status!=BluetoothGatt.GATT_SUCCESS) { fail("No se pudieron leer los servicios."); return; }
                BluetoothGattService service=g.getService(HapticProtocol.SERVICE);
                commandCharacteristic=service==null ? null : service.getCharacteristic(HapticProtocol.COMMAND);
                BluetoothGattCharacteristic notify=service==null ? null : service.getCharacteristic(HapticProtocol.STATUS);
                if (commandCharacteristic==null || notify==null
                        || (commandCharacteristic.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE)==0
                        || (notify.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY)==0) {
                    fail("Firmware incompatible. Carga el firmware Vibrafil compatible."); return;
                }
                BluetoothGattCharacteristic caps=service.getCharacteristic(HapticProtocol.CAPABILITIES);
                if(caps==null) { subscribe(g); return; } // Original dual firmware.
                try {
                    if((caps.getProperties()&BluetoothGattCharacteristic.PROPERTY_READ)==0 || !g.readCharacteristic(caps))
                        fail("No se pudieron consultar los canales disponibles.");
                } catch(RuntimeException e) { fail("Error al leer capacidades Bluetooth."); }
            });
        }
        @Override public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value,int status) {
            byte[] copy=value==null ? new byte[0] : value.clone(); main.post(() -> capabilitiesRead(g,c,copy,status));
        }
        @Override @SuppressWarnings("deprecation") public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,int status) {
            if(Build.VERSION.SDK_INT<33) {
                byte[] value=c.getValue(); byte[] copy=value==null ? new byte[0] : value.clone();
                main.post(() -> capabilitiesRead(g,c,copy,status));
            }
        }
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor descriptor,int status) {
            main.post(() -> {
                if (g!=gatt || !HapticProtocol.CCCD.equals(descriptor.getUuid())) return;
                if (status!=BluetoothGatt.GATT_SUCCESS) { fail("Suscripción rechazada: "+status); return; }
                verifying=true;
                state("Verificando recepción con una orden de apagado…");
                queue.offer(0,0,0,SystemClock.elapsedRealtime()); pump();
            });
        }
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic characteristic,int status) {
            main.post(() -> {
                if (g!=gatt || flight==null || !HapticProtocol.COMMAND.equals(characteristic.getUuid())) return;
                if (status!=BluetoothGatt.GATT_SUCCESS) { fail("Android rechazó la escritura: "+status); return; }
                flight.written=true; complete();
            });
        }
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value) {
            byte[] copy=value.clone(); main.post(() -> received(g,c,copy));
        }
        @Override @SuppressWarnings("deprecation") public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c) {
            if (Build.VERSION.SDK_INT<33) {
                byte[] raw=c.getValue(); byte[] copy=raw==null ? new byte[0] : raw.clone();
                main.post(() -> received(g,c,copy));
            }
        }
    };
    private void capabilitiesRead(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value,int status) {
        if(g!=gatt) return;
        if(HapticProtocol.LEVEL_CAPABILITIES.equals(c.getUuid())) {
            levelControlAvailable=status==BluetoothGatt.GATT_SUCCESS && HapticProtocol.supportsLevelControl(value);
            if(!levelControlAvailable) { fail("Control de intensidad inválido. Revisa el firmware."); return; }
            subscribe(g); return;
        }
        if(HapticProtocol.MUSICAL_CAPABILITIES.equals(c.getUuid())) {
            musicalAvailable=status==BluetoothGatt.GATT_SUCCESS && HapticProtocol.configureMusicalModel(value);
            if(!musicalAvailable) { fail("Capacidades musicales inválidas."); return; }
            BluetoothGattCharacteristic levels=g.getService(HapticProtocol.SERVICE).getCharacteristic(HapticProtocol.LEVEL_CAPABILITIES);
            if(levels==null) { subscribe(g); return; }
            try { if(!g.readCharacteristic(levels)) fail("No se pudo comprobar el control de intensidad."); }
            catch(RuntimeException e) { fail("Error al leer el control de intensidad."); }
            return;
        }
        if(!HapticProtocol.CAPABILITIES.equals(c.getUuid())) return;
        HapticProtocol.Capabilities caps=HapticProtocol.capabilities(value);
        if(status!=BluetoothGatt.GATT_SUCCESS || caps==null) { fail("Capacidades del firmware inválidas."); return; }
        protocolVersion=caps.protocol; centerAvailable=caps.centerAvailable;
        BluetoothGattCharacteristic musical=g.getService(HapticProtocol.SERVICE).getCharacteristic(HapticProtocol.MUSICAL_CAPABILITIES);
        if(musical==null) { subscribe(g); return; }
        try { if(!g.readCharacteristic(musical)) fail("No se pudieron leer las frecuencias disponibles."); }
        catch(RuntimeException e) { fail("Error al leer capacidades musicales."); }
    }
    @SuppressWarnings("deprecation") private void subscribe(BluetoothGatt g) {
        BluetoothGattCharacteristic notify=g.getService(HapticProtocol.SERVICE).getCharacteristic(HapticProtocol.STATUS);
        BluetoothGattDescriptor descriptor=notify.getDescriptor(HapticProtocol.CCCD);
        try {
            if(descriptor==null || !g.setCharacteristicNotification(notify,true)) { fail("Faltan notificaciones de confirmación."); return; }
            boolean ok;
            if(Build.VERSION.SDK_INT>=33) ok=g.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)==BluetoothStatusCodes.SUCCESS;
            else { descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); ok=g.writeDescriptor(descriptor); }
            if(!ok) fail("No se pudo activar la confirmación del ESP32.");
        } catch(RuntimeException e) { fail("Error al suscribirse: "+e.getMessage()); }
    }
    private void received(BluetoothGatt g,BluetoothGattCharacteristic characteristic,byte[] bytes) {
        if (g!=gatt || !HapticProtocol.STATUS.equals(characteristic.getUuid())) return;
        HapticProtocol.Ack ack=HapticProtocol.decodeAck(bytes);
        if (ack==null || flight==null || ack.sequence!=flight.sequence || ack.version!=flight.payload[1]) return;
        if (ack.result==HapticProtocol.STALE_COMMAND && !verifying) {
            // A late note is intentionally skipped; do not replay it or disconnect.
            flight.acknowledged=true; listener.onAck(ack); complete(); return;
        }
        if (ack.result!=HapticProtocol.OK) {
            fail(ack.result==HapticProtocol.CENTER_UNAVAILABLE ? "El canal central no está habilitado o ha fallado en el ESP32."
                : "El firmware rechazó el comando: "+ack.result); return;
        }
        byte[] sent=flight.payload;
        if (!ack.matches(sent)) {
            fail("Confirmación distinta de la orden enviada."); return;
        }
        flight.acknowledged=true; listener.onAck(ack); complete();
    }
    private void complete() {
        if (flight==null || !flight.written || !flight.acknowledged) return;
        main.removeCallbacks(writeTimeout); flight=null;
        if (verifying) {
            verifying=false; ready=true;
            main.removeCallbacks(connectTimeout); state(levelControlAvailable ? "Brazalete conectado y listo" : "Conectado. Actualiza el firmware a v5.1 para el control completo.");
        }
        pump();
    }
    public boolean send(int left,int right,int duration) { return send(left,right,0,0,duration); }
    public boolean send(int left,int right,int center,int hz,int duration) {
        if(!ready || center != 0 || hz != 0) return false;
        HapticProtocol.commandV2(0,left,right,center,hz,duration);
        queue.offer(left,right,center,hz,duration,SystemClock.elapsedRealtime()); pump(); return true;
    }
    public boolean sendMusical(int left,int right,int center,int carrierCentiHz,int duration) {
        if(!ready || !musicalAvailable || center != 0) return false;
        HapticProtocol.commandV3(0,left,right,center,carrierCentiHz,duration);
        queue.offer(left,right,center,center>0 ? Math.round(carrierCentiHz/100.0f) : 0,
            duration,SystemClock.elapsedRealtime(),carrierCentiHz); pump(); return true;
    }
    public boolean sendControlled(int left,int right,int carrierCentiHz,int intensity,int duration) {
        if(!ready || !levelControlAvailable || !musicalAvailable) return false;
        HapticProtocol.commandV4(0,left,right,carrierCentiHz,intensity,duration);
        if(intensity==0) { stop(); return true; }
        queue.offer(left,right,0,0,duration,SystemClock.elapsedRealtime(),carrierCentiHz,intensity);
        pump(); return true;
    }
    public void stop() { if (ready) { queue.offer(0,0,0,SystemClock.elapsedRealtime()); pump(); } }
    @SuppressWarnings("deprecation") private void pump() {
        if ((!ready && !verifying) || gatt==null || flight!=null) return;
        HapticCommandQueue.Entry p=queue.poll(SystemClock.elapsedRealtime());
        if(p==null) return;
        sequence=(sequence+1)&65535;
        byte[] bytes=p.carrierCentiHz>0 && levelControlAvailable
            ? HapticProtocol.commandV4(sequence,p.left,p.right,p.carrierCentiHz,p.intensity,p.ms)
            : p.carrierCentiHz>0 && musicalAvailable
            ? HapticProtocol.commandV3(sequence,p.left,p.right,p.center,p.carrierCentiHz,p.ms)
            : protocolVersion==2
            ? HapticProtocol.commandV2(sequence,p.left,p.right,p.center,p.hz,p.ms)
            : HapticProtocol.command(sequence,p.left,p.right,p.ms);
        flight=new Flight(sequence,bytes);
        try {
            boolean ok;
            if (Build.VERSION.SDK_INT>=33) ok=gatt.writeCharacteristic(commandCharacteristic,bytes,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)==BluetoothStatusCodes.SUCCESS;
            else {
                commandCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                commandCharacteristic.setValue(bytes); ok=gatt.writeCharacteristic(commandCharacteristic);
            }
            if (!ok) { fail("No se pudo iniciar la escritura BLE."); return; }
            main.postDelayed(writeTimeout,1500);
        } catch (RuntimeException e) { fail("Error al escribir: "+e.getMessage()); }
    }
    private void fail(String message) { disconnect(); state(message); }
    public void disconnect() {
        stopScan(); main.removeCallbacks(connectTimeout); main.removeCallbacks(writeTimeout);
        BluetoothGatt old=gatt; gatt=null;
        ready=false; verifying=false; centerAvailable=false; musicalAvailable=false; levelControlAvailable=false; protocolVersion=1; flight=null; queue.clear(); commandCharacteristic=null;
        if (old!=null) {
            try { old.disconnect(); } catch (RuntimeException ignored) {}
            try { old.close(); } catch (RuntimeException ignored) {}
        }
        state("Desconectado");
    }
    public void close() { disconnect(); main.removeCallbacksAndMessages(null); }
}
