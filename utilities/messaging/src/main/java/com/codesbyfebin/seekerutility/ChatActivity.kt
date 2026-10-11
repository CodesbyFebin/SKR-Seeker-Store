package com.codesbyfebin.seekerutility
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.io.DataInputStream
import java.io.DataOutputStream

/** Foreground same-LAN chat, not Wi-Fi Direct discovery, Internet messaging or an audited messenger. */
class ChatActivity:ComponentActivity(){
 private var socket:Socket?=null;private var server:ServerSocket?=null;private var connected by mutableStateOf(false);private var working by mutableStateOf(false);private var status by mutableStateOf("Disconnected");private var messages by mutableStateOf<List<String>>(emptyList());private var secret:CharArray?=null;private val sendLock=Any()
 private fun privateAddress(text:String):Boolean {val a=text.split('.').map{it.toIntOrNull()?:return false};return a.size==4 && a.all{it in 0..255} && (a[0]==10 || (a[0]==192 && a[1]==168) || (a[0]==172 && a[1] in 16..31))}
 private fun disconnect(){socket?.close();server?.close();socket=null;server=null;secret?.fill('\u0000');secret=null;connected=false;working=false;status="Disconnected"}
 private fun connect(host:String?,password:String){if(working)return;require(password.length>=12);working=true;secret=password.toCharArray();status=if(host==null)"Listening on LAN port 43865" else "Connecting…"
 lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){val s=if(host==null){val listener=ServerSocket();server=listener;listener.bind(InetSocketAddress(43865));val accepted=listener.accept();listener.close();server=null;require(privateAddress(accepted.inetAddress.hostAddress?:""));accepted}else {require(privateAddress(host));Socket().apply{connect(InetSocketAddress(host,43865),10000)}};socket=s;s.soTimeout=60000;withContext(Dispatchers.Main){connected=true;status="Connected; exchange messages to verify shared passphrase"};val input=DataInputStream(s.getInputStream());while(!s.isClosed){val length=input.readInt();require(length in 48..8192);val bytes=ByteArray(length);input.readFully(bytes);val key=secret?.copyOf()?:error("Disconnected");val text=try{PortableBackup.decrypt(bytes,key).toString(Charsets.UTF_8)}finally{key.fill('\u0000')};withContext(Dispatchers.Main){messages=(messages+"Peer: $text").takeLast(100);status="Authenticated message received"}}}}.onFailure{status="Connection ended or message authentication failed"};disconnect()}
 }
 override fun onStop(){super.onStop();disconnect();messages=emptyList()}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
 setContent{MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 var host by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var text by remember{mutableStateOf("")}
 Text("Seeker Nearby Chat",style=MaterialTheme.typography.headlineMedium);Text("Use the same Wi-Fi or hotspot, no Internet or phone number. Share a strong passphrase in person. Manual private IPv4 connection; no automatic discovery. Foreground only; messages are not saved.")
 OutlinedTextField(host,{host=it},label={Text("Peer's private IPv4 address")});OutlinedTextField(pass,{pass=it},label={Text("Shared passphrase (12+ characters)")},visualTransformation=PasswordVisualTransformation())
 Row{Button(enabled=!working && pass.length>=12,onClick={connect(null,pass)}){Text("Host")};Spacer(Modifier.width(8.dp));Button(enabled=!working && pass.length>=12 && privateAddress(host),onClick={connect(host,pass)}){Text("Connect")};TextButton(onClick={disconnect()}){Text("End")}}
 Text(status);LazyColumn(Modifier.weight(1f)){items(messages){Text(it)}}
 OutlinedTextField(text,{text=it.take(1000)},label={Text("Message")});Button(enabled=connected && text.isNotBlank(),onClick={val value=text;text="";lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){val key=secret?.copyOf()?:error("Disconnected");val bytes=try{PortableBackup.encrypt(value.toByteArray(),key)}finally{key.fill('\u0000')};synchronized(sendLock){val output=DataOutputStream(socket?.getOutputStream()?:error("Disconnected"));output.writeInt(bytes.size);output.write(bytes);output.flush()}}}.onSuccess{messages=(messages+"You: $value").takeLast(100)}.onFailure{status="Send failed"}}}){Text("Send")}
 }}}}
 }
}
