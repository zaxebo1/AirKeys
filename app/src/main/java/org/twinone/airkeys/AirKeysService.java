package org.twinone.airkeys;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import com.google.gson.Gson;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import java.io.File;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class AirKeysService extends InputMethodService {

    private static final int MAX_CLIENTS = 1;

    private static final String ACTION_TEXT = "text";
    private static final String ACTION_ENTER = "enter";
    private static final String ACTION_BACKSPACE = "backspace";

    private static final boolean FORCE_RELOAD = true;
    private static final String TAG = AirKeysService.class.getSimpleName();
    private static final String WEBAPP_SOURCE = "root.war";
    private static final String WEBAPPS_DIR = "webapps";
    private static final String WEBAPP_NAME = "root.war";


    private TextView mMessage;
    private InputServer mServer;

    private static final int HTTP_PORT = 8080; // TODO support ssl?

    public AirKeysService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "OnCreate");
        mServer = new InputServer();
        extractAssetsIfNecessary();
        startServer();
        super.onCreate();
    }


    private void startServer() {
        try {
            mServer.start();
        } catch (Exception e) {
            Log.w(TAG, "Exception starting Jetty", e);
        }
    }

    private void stopServer() {
        try {
            mServer.stop();
        } catch (Exception e) {
            Log.w(TAG, "Exception stopping Jetty", e);
        }
    }

    @Override
    public View onCreateInputView() {
        ViewGroup vg = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.keyboard, null);
        mMessage = (TextView) vg.findViewById(R.id.keyboard_text);
        updateClients();
        return vg;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "OnDestroy");
        super.onDestroy();
    }


    private class InputServer extends Server {
        public InputServer() {
            super();

            // HTTP
            SelectChannelConnector httpConnector = new SelectChannelConnector();
            httpConnector.setPort(HTTP_PORT);
            setConnectors(new Connector[]{httpConnector});

            Log.d(TAG, "Webapp location: " + getWebAppLocation().getAbsolutePath());
            WebAppContext wac = new WebAppContext();
            wac.setContextPath("/");
            wac.setWar(getWebappFile().getAbsolutePath());
            wac.setExtractWAR(true);
//            ServletHolder sh = new ServletHolder(new InputServlet());
            wac.addServlet(new ServletHolder(mInputServlet), "/ws");

            setHandler(wac);
        }
    }

    private void extractAssetsIfNecessary() {
        File dir = getWebAppLocation();
        if (!dir.exists() || !dir.isDirectory() || FORCE_RELOAD) {
            Log.i(TAG, "Extracting web app from assets");
            FileUtil.copyAssetsFile(this, WEBAPP_SOURCE, getWebappFile().getAbsolutePath());
        } else {
            Log.d(TAG, "Webapp already extracted");
        }
    }

    private File getWebAppLocation() {
        return getDir(WEBAPPS_DIR, Context.MODE_PRIVATE);
    }

    private File getWebappFile() {
        return new File(getWebAppLocation(), WEBAPP_NAME);
    }


    private InputServlet mInputServlet = new InputServlet();

    private ArrayList<WSClient> mClients = new ArrayList<>();

    public class WSClient implements WebSocket.OnTextMessage {
        public WSClient(String host) {
            this.host = host;
        }

        public int id;
        private String host;
        private Connection mConnection;

        @Override
        public void onOpen(Connection connection) {
            Log.d(TAG, "OnOpen");
            mConnection = connection;
            mClients.add(this);
            updateClients();
        }

        @Override
        public void onClose(int i, String s) {
            Log.d(TAG, "OnClose");
            mClients.remove(this);
            updateClients();
        }

        @Override
        public void onMessage(String s) {
            Log.d(TAG, "OnMessage: " + s);
            ClientMessage m = new Gson().fromJson(s, ClientMessage.class);
            AirKeysService.this.onMessage(this, m);
        }

    }


    public class InputServlet extends WebSocketServlet {

        public InputServlet() {

        }

        @Override
        public void init() throws ServletException {
            super.init();
        }

        @Override
        public WebSocket doWebSocketConnect(HttpServletRequest req, String s) {
            if (mClients.size() > MAX_CLIENTS) {
                Log.w(TAG, "Maximum client number reached: " + MAX_CLIENTS);
                return null;
            }
            WSClient c = new WSClient(req.getRemoteHost());
            return c;
        }
    }


    private void sendMessage(ServerMessage m) {
        for (WSClient c : mClients) {
            try {
                c.mConnection.sendMessage(new Gson().toJson(m));
            } catch (Exception e) {
                Log.w(TAG, "Failed to send message to a client");
            }
        }

    }

    class ClientMessage {
        String text;
        String action;
    }

    class ServerMessage {
        String text;
    }

    private void onMessage(WSClient client, ClientMessage m) {
        InputConnection conn = getCurrentInputConnection();

        if (conn == null) Log.w(TAG, "InputConnection null");
        switch (m.action) {
            case ACTION_TEXT:
                conn.deleteSurroundingText(Integer.MAX_VALUE, Integer.MAX_VALUE);
                conn.commitText(m.text, m.text.length());
                break;
            case ACTION_ENTER:
                KeyEvent enterDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER);
                conn.sendKeyEvent(enterDown);
                KeyEvent enterUp = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER);
                conn.sendKeyEvent(enterUp);
        }
    }

    private void updateClients() {
        if (mMessage == null) return;
        mMessage.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Setting text");
                synchronized (mClients) {
                    if (mClients.size() > 0) {
                        mMessage.setText("Conn: " + mClients.get(0).host);
                    } else {
                        mMessage.setText("No clients connected");
                    }
                }
            }
        });
    }

}
