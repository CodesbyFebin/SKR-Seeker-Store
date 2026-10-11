package com.codesbyfebin.seekerutility
import org.junit.Test
import org.junit.Assert.*
class UtilityCoreTest {
 @Test fun precedenceAndUnary(){assertEquals(14.0,Calculator.evaluate("2+3*4"),0.0);assertEquals(-20.0,Calculator.evaluate("-(2+3)*4"),0.0)}
 @Test fun rejectBrokenExpressions(){for(s in listOf("1/0","1+","(2+3","2abc","1..2","")){try{Calculator.evaluate(s);fail(s)}catch(_:IllegalArgumentException){}}}
 @Test fun encryptedBackupRoundtrip(){val bytes="Offline private file".toByteArray();val encoded=PortableBackup.encrypt(bytes,"correct long passphrase".toCharArray());assertArrayEquals(bytes,PortableBackup.decrypt(encoded,"correct long passphrase".toCharArray()));assertFalse(encoded.contentEquals(bytes))}
 @Test fun rejectTamperAndWrongPassword(){val encoded=PortableBackup.encrypt("file".toByteArray(),"correct long passphrase".toCharArray());try{PortableBackup.decrypt(encoded,"incorrect long passphrase".toCharArray());fail()}catch(_:javax.crypto.AEADBadTagException){};encoded[encoded.lastIndex]=(encoded.last().toInt() xor 1).toByte();try{PortableBackup.decrypt(encoded,"correct long passphrase".toCharArray());fail()}catch(_:javax.crypto.AEADBadTagException){}}
}
