package com.example.rootscriptexecutor

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.RemoteException

interface IRootService : IInterface {
    
    @Throws(RemoteException::class)
    fun executeScript(scriptName: String): String
    
    @Throws(RemoteException::class)
    fun checkScriptExists(scriptName: String): Boolean
    
    @Throws(RemoteException::class)
    fun listScripts(): Array<String>
    
    @Throws(RemoteException::class)
    fun uploadScript(scriptName: String, content: ByteArray): Boolean
    
    abstract class Stub : Binder(), IRootService {
        
        companion object {
            private const val DESCRIPTOR = "com.example.rootscriptexecutor.IRootService"
            
            private const val TRANSACTION_executeScript = IBinder.FIRST_CALL_TRANSACTION
            private const val TRANSACTION_checkScriptExists = IBinder.FIRST_CALL_TRANSACTION + 1
            private const val TRANSACTION_listScripts = IBinder.FIRST_CALL_TRANSACTION + 2
            private const val TRANSACTION_uploadScript = IBinder.FIRST_CALL_TRANSACTION + 3
            
            fun asInterface(binder: IBinder): IRootService? {
                if (binder == null) return null
                val iin = binder.queryLocalInterface(DESCRIPTOR)
                return if (iin != null && iin is IRootService) {
                    iin
                } else {
                    Proxy(binder)
                }
            }
        }
        
        init {
            attachInterface(this, DESCRIPTOR)
        }
        
        override fun asBinder(): IBinder = this
        
        @Throws(RemoteException::class)
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            when (code) {
                INTERFACE_TRANSACTION -> {
                    reply?.writeString(DESCRIPTOR)
                    return true
                }
                TRANSACTION_executeScript -> {
                    data.enforceInterface(DESCRIPTOR)
                    val scriptName = data.readString() ?: ""
                    val result = executeScript(scriptName)
                    reply?.writeNoException()
                    reply?.writeString(result)
                    return true
                }
                TRANSACTION_checkScriptExists -> {
                    data.enforceInterface(DESCRIPTOR)
                    val scriptName = data.readString() ?: ""
                    val result = checkScriptExists(scriptName)
                    reply?.writeNoException()
                    reply?.writeByte(if (result) 1 else 0)
                    return true
                }
                TRANSACTION_listScripts -> {
                    data.enforceInterface(DESCRIPTOR)
                    val result = listScripts()
                    reply?.writeNoException()
                    reply?.writeStringArray(result)
                    return true
                }
                TRANSACTION_uploadScript -> {
                    data.enforceInterface(DESCRIPTOR)
                    val scriptName = data.readString() ?: ""
                    val content = data.createByteArray() ?: ByteArray(0)
                    val result = uploadScript(scriptName, content)
                    reply?.writeNoException()
                    reply?.writeByte(if (result) 1 else 0)
                    return true
                }
            }
            return super.onTransact(code, data, reply, flags)
        }
        
        private class Proxy(private val remote: IBinder) : IRootService {
            
            override fun asBinder(): IBinder = remote
            
            @Throws(RemoteException::class)
            override fun executeScript(scriptName: String): String {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeString(scriptName)
                    remote.transact(TRANSACTION_executeScript, data, reply, 0)
                    reply.readException()
                    reply.readString() ?: ""
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            }
            
            @Throws(RemoteException::class)
            override fun checkScriptExists(scriptName: String): Boolean {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeString(scriptName)
                    remote.transact(TRANSACTION_checkScriptExists, data, reply, 0)
                    reply.readException()
                    reply.readByte() != 0.toByte()
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            }
            
            @Throws(RemoteException::class)
            override fun listScripts(): Array<String> {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    remote.transact(TRANSACTION_listScripts, data, reply, 0)
                    reply.readException()
                    reply.createStringArray() ?: emptyArray()
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            }
            
            @Throws(RemoteException::class)
            override fun uploadScript(scriptName: String, content: ByteArray): Boolean {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeString(scriptName)
                    data.writeByteArray(content)
                    remote.transact(TRANSACTION_uploadScript, data, reply, 0)
                    reply.readException()
                    reply.readByte() != 0.toByte()
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            }
        }
    }
}