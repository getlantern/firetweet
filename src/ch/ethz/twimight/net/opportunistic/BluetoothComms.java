/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ethz.twimight.net.opportunistic;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import ch.ethz.twimight.R;
import ch.ethz.twimight.util.Constants;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothComms{
    // Debugging
    private static final String TAG = "BluetoothComms";
    private static final String T = "btdebug";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_INSECURE = "BluetoothComms";

    // Unique UUID for this application   
    private static final UUID MY_UUID_INSECURE =
    		UUID.fromString("8113ac40-438f-11e1-b86c-0800200c9a66");        

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private final Context mContext;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    
    ObjectOutputStream out;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothComms( Context context, Handler handler) {
    	mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

       
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        Log.d(T, "BluetoothComms.start()");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}       

        // Start the thread to listen on a BluetoothServerSocket        
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        try {
        	mInsecureAcceptThread = new AcceptThread();        	
        	mInsecureAcceptThread.start();        
        	
        	setState(STATE_LISTEN);
        } catch (IOException e) {
        	Log.e(T,"listen() failed", e);
        	Message msg = mHandler.obtainMessage(Constants.BLUETOOTH_RESTART, -1, -1, null);
        	mHandler.sendMessage(msg);
        	Log.d("btdebug", "restarting bluetooth");
        }
           
        
    }
    
    

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(String mac) {
        
    	Log.d(T, "BluetoothComms.connect() " + mac);
        
        // Which device??
        BluetoothDevice device = mAdapter.getRemoteDevice(mac);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
       // if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        
        if (mConnectedThread == null) {

        	// Start the thread to connect with the given device
       	 mConnectThread = new ConnectThread(device);
       	 mConnectThread.start();
       	 setState(STATE_CONNECTING);
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(T, "connected() " + device.getName() + " (" + device.getAddress() + ")");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        
        if (mState != STATE_CONNECTED ) {

        	// Start the thread to manage the connection and perform transmissions
        	mConnectedThread = new ConnectedThread(socket);
        	mConnectedThread.start();
        	
        	setState(STATE_CONNECTED);
        	// Send the name of the connected device back to the UI Activity
        	Message msg = mHandler.obtainMessage(Constants.MESSAGE_CONNECTION_SUCCEEDED, -1, -1, device.getAddress());
        	mHandler.sendMessage(msg);

        	
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }      

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(String out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed(String mac) {
        // Send a failure message back to the Activity
       
    	// Send a failure message back to the Activity
    	Log.d("btdebug", "connectionFailed() " + mac);
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_CONNECTION_FAILED, -1 ,-1, mac);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
    	// Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_CONNECTION_LOST);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        

        public AcceptThread() throws IOException {
            BluetoothServerSocket tmp = null;          

            // Create a new listening server socket
                         
            tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);         
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(T, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");

            BluetoothSocket socket = null;
            
          
            	// Listen to the server socket if we're not connected
                while (mState != STATE_CONNECTED) {
                    try {
                        // This is a blocking call and will only return on a
                        // successful connection or an exception
                    	BluetoothStatus.getInstance().setStatusDescription(mContext.getString(R.string.btstatus_listening));
                        socket = mmServerSocket.accept();
                        Log.i(T,"connection received, socket created");
                    } catch (IOException e) {
                        // Log.e(TAG, "accept() failed");
                        break;
                    }

                    // If a connection was accepted
                    if (socket != null) {
                        synchronized (BluetoothComms.this) {
                            switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                            	mAdapter.cancelDiscovery();
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                            }
                        }
                    }
                }
            
            
            if (D) Log.i(TAG, "END mAcceptThread " );

        }

        public void cancel() {
            
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;        

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;           
            Log.d(T, "ConnectThread()");
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                 
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
                Log.e("btdebug", "create() failed");
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.d(T, "BEGIN mConnectThread " + mmDevice.getName() + " (" + mmDevice.getAddress() + ")");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
            	Log.d("btdebug", "discovering: " +mAdapter.isDiscovering());
            	String remoteDeviceName = mmDevice.getName()!=null ? mmDevice.getName() : "?";
            	String statusDescription = String.format(mContext.getString(R.string.btstatus_connecting_to), remoteDeviceName);
            	BluetoothStatus.getInstance().setStatusDescription(statusDescription);
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() ", e2);
                }
                Log.e(T, "connect() failed", e);
                connectionFailed(mmDevice.getAddress());
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothComms.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect ", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(T, "create ConnectedThread" );
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(T, "BEGIN mConnectedThread");
            String remoteDeviceName = mmSocket.getRemoteDevice().getName()!=null ? mmSocket.getRemoteDevice().getName() : "?"; 
            String statusDescription = String.format(mContext.getString(R.string.btstatus_connected_to), remoteDeviceName);
            BluetoothStatus.getInstance().setStatusDescription(statusDescription);
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                	ObjectInputStream in = new ObjectInputStream(mmInStream);
                    Object buffer;
                    
                    buffer = in.readObject();
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, -1, -1, buffer)
                    .sendToTarget();
                    
                } catch (IOException e) {
                    Log.e(TAG, "disconnected");
                    connectionLost();
                    break;
                } catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            Log.d(T, "ConnectedThread finished");
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        //not to write String, but Object
        public void write(String buffer) {
            try {
            	out = new ObjectOutputStream(mmOutStream);
                // Send it
                out.writeObject(buffer);                
                out.flush(); 
                
            } catch (IOException e) {
                Log.e(T, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
            	Log.d("btdebug", "closing inputStream");
            	mmInStream.close();
            	Log.d("btdebug", "closing outputStream");
            	mmOutStream.close();
            	Log.d("btdebug", "closing socket");
                mmSocket.close();
                Log.d("btdebug", "closing socket ConnectedThread");
            } catch (IOException e) {
                Log.e("btdebug", "close() of connect socket failed");
            }
        }
    }
}
