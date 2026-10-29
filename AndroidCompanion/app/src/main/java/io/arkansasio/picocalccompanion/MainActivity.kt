package io.arkansasio.picocalccompanion

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : Activity() {
 private lateinit var root: LinearLayout
 private lateinit var page: LinearLayout
 private lateinit var status: TextView
 private var port: UsbSerialPort? = null
 private var connection: android.hardware.usb.UsbDeviceConnection? = null
 private var io: SerialInputOutputManager? = null
 private val executor = Executors.newSingleThreadExecutor()
 private var output: TextView? = null
 private var code: EditText? = null
 private var folder: Uri? = null
 private var keyCount = 0
 private val permissionAction = "io.arkansasio.picocalccompanion.USB_PERMISSION"
 private val receiver = object : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
   if (intent?.action != permissionAction) return
   @Suppress("DEPRECATION")
   val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
   if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) connect(device)
   else status("USB permission denied")
  }
 }
 private val usb by lazy { getSystemService(Context.USB_SERVICE) as UsbManager }

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val filter = IntentFilter(permissionAction)
  if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED) else registerReceiver(receiver, filter)
  root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 8, 12, 8); setBackgroundColor(0xFF101923.toInt()) }
  root.addView(label("PICOCALC / ANDROID COMPANION", 20f))
  val nav = LinearLayout(this)
  listOf("Calc","Terminal","Files","MicroPython","Emulator","USB").forEach { item ->
   nav.addView(Button(this).apply { text = item; textSize = 10f; setOnClickListener { when(item) {
    "Calc" -> calculator(); "Terminal" -> terminal(); "Files" -> files(); "MicroPython" -> micropython(); "Emulator" -> emulator(); else -> usbPage()
   } } }, LinearLayout.LayoutParams(0, -2, 1f))
  }
  root.addView(nav)
  status = label("Ready · USB disconnected", 12f); root.addView(status)
  val scroll = ScrollView(this)
  page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
  scroll.addView(page); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
  setContentView(root); calculator()
 }
 private fun label(text: String, size: Float = 14f) = TextView(this).apply {
  this.text = text; textSize = size; setTextColor(0xFFE1EAF2.toInt()); setPadding(4, 8, 4, 8)
 }
 private fun field(hintText: String, multi: Boolean = false) = EditText(this).apply {
  hint = hintText; textSize = 16f; setTextColor(-1); setHintTextColor(0xFF93A6B7.toInt()); setPadding(10, 8, 10, 8)
  setBackgroundColor(0xFF1D2A38.toInt())
  if (multi) { minLines = 7; gravity = Gravity.TOP; inputType = 1 or 0x20000 } else inputType = 1
  layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 6 }
 }
 private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; setOnClickListener { action() } }
 private fun base(title: String, help: String) { page.removeAllViews(); page.addView(label(title, 22f)); page.addView(label(help)) }
 private fun status(message: String) { status.text = message; Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }

 private fun calculator() {
  base("Scientific calculator", "Arithmetic · precedence · parentheses · decimals · exponent notation")
  val input = field("Example: (12 + 8) / 4"); val result = label("Result appears here")
  page.addView(input); page.addView(button("Calculate") { try { result.text = "Result: " + ExpressionEvaluator.evaluate(input.text.toString()) } catch(e: Exception) { result.text = "Error: " + (e.message ?: "invalid expression") } })
  page.addView(result); page.addView(button("Insert π") { input.append("3.141592653589793") })
 }
 private fun terminal() {
  base("Serial terminal", "Sends UTF-8 text with CRLF to a supported USB serial device.")
  output = label("Terminal output will appear here."); page.addView(output)
  val input = field("Command or text to send"); page.addView(input)
  page.addView(button("Send line") { try {
   val message = input.text.toString(); if (message.isBlank()) throw IOException("Enter a command")
   (port ?: throw IOException("Connect USB first")).write((message + "\r\n").toByteArray(), 1500)
   output?.append("\n> " + message)
  } catch(e: Exception) { status("Send failed: " + e.message) } })
  page.addView(button("Clear output") { output?.text = "" })
 }
 private fun files() {
  base("File access", "Android Storage Access Framework. Choose a folder to view its entries, or save/import files using system pickers.")
  page.addView(button("Choose folder") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 410) })
  page.addView(button("List selected folder") { listFolder() })
  page.addView(button("Import file") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, 411) })
 }
 private fun listFolder() {
  val uri = folder ?: run { status("Choose a folder first"); return }
  val entries = DocumentFile.fromTreeUri(this, uri)?.listFiles()?.joinToString("\n") {
   (if (it.isDirectory) "[DIR] " else "[FILE] ") + (it.name ?: "(unnamed)")
  }.orEmpty()
  base("Folder contents", if (entries.isBlank()) "Empty or inaccessible folder" else entries)
 }
 private fun micropython() {
  base("MicroPython workspace", "Text editor and serial send. Device must already be at a compatible REPL; this is not robust file transfer.")
  code = field("Python script", true); code?.setText("# PicoCalc MicroPython script\nprint('Hello from Android')\n"); page.addView(code)
  page.addView(button("Send script to REPL") { try {
   (port ?: throw IOException("Connect USB first")).write((code?.text.toString() + "\r\n").toByteArray(), 2000)
   status("Script text sent; verify the target REPL state.")
  } catch(e: Exception) { status("Send failed: " + e.message) } })
  page.addView(button("Save script") { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
   addCategory(Intent.CATEGORY_OPENABLE); type = "text/x-python"; putExtra(Intent.EXTRA_TITLE, "main.py")
  }, 412) })
 }
 private fun emulator() {
  base("PicoCalc screen simulator", "UI mock only; it does not emulate RP2040/RP2350 instructions or firmware.")
  val screen = label("┌────────────────────────────┐\n│         PicoCalc           │\n│                            │\n│  Ready for input           │\n└────────────────────────────┘", 16f)
  screen.typeface = android.graphics.Typeface.MONOSPACE; page.addView(screen)
  val keys = LinearLayout(this)
  listOf("↑","←","OK","→","↓").forEach { k -> keys.addView(Button(this).apply { text = k; setOnClickListener {
   keyCount++; screen.text = "PicoCalc Simulator\nKey: " + k + "\nEvents: " + keyCount
  } }, LinearLayout.LayoutParams(0, -2, 1f)) }
  page.addView(keys)
 }
 private fun usbPage() {
  base("USB device manager", "Requires USB host/OTG, a data cable, and a supported USB-to-serial bridge.")
  val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usb)
  page.addView(label(if (drivers.isEmpty()) "No supported serial device detected." else drivers.mapIndexed { i, d -> "$i · " + d.device.deviceName + " · " + d.ports.size + " port(s)" }.joinToString("\n")))
  drivers.forEach { driver -> page.addView(button("Connect " + driver.device.deviceName) {
   if (usb.hasPermission(driver.device)) connect(driver.device) else {
    val pending = PendingIntent.getBroadcast(this, 0, Intent(permissionAction).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    usb.requestPermission(driver.device, pending)
   }
  }) }
  page.addView(button("Disconnect") { disconnect(); status("USB disconnected") })
 }
 private fun connect(device: UsbDevice) {
  try {
   val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usb).firstOrNull { it.device.deviceId == device.deviceId } ?: throw IOException("Unsupported serial device")
   disconnect()
   val conn = usb.openDevice(device) ?: throw IOException("Cannot open USB device")
   val p = driver.ports.firstOrNull() ?: throw IOException("No serial ports")
   p.open(conn); p.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
   connection = conn; port = p
   io = SerialInputOutputManager(p, object : SerialInputOutputManager.Listener {
    override fun onNewData(data: ByteArray) { runOnUiThread { output?.append(String(data, Charsets.UTF_8)) } }
    override fun onRunError(e: Exception) { runOnUiThread { status("Serial read error: " + e.message) } }
   }).also { executor.submit(it) }
   status("Connected: " + device.deviceName + " · 115200 8N1")
  } catch(e: Exception) { disconnect(); status("USB connection failed: " + e.message) }
 }
 private fun disconnect() {
  io?.stop(); io = null
  try { port?.close() } catch(_: Exception) {}
  port = null
  try { connection?.close() } catch(_: Exception) {}
  connection = null
 }
 @Deprecated("Kept simple for compatibility")
 override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  super.onActivityResult(requestCode, resultCode, data)
  if (resultCode != RESULT_OK || data?.data == null) return
  val uri = data.data!!
  when(requestCode) {
   410 -> { folder = uri; try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch(_: Exception) {}; listFolder() }
   411 -> status("File selected: " + (uri.lastPathSegment ?: uri.toString()))
   412 -> try { contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(code?.text.toString()) }; status("Script saved") } catch(e: Exception) { status("Save failed: " + e.message) }
  }
 }
 override fun onDestroy() {
  disconnect(); try { unregisterReceiver(receiver) } catch(_: Exception) {}; executor.shutdownNow(); super.onDestroy()
 }
}
