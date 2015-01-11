package org.twinone.airkeys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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

    // Client actions
    private static final String ACTION_TEXT = "text";
    private static final String ACTION_COMMIT = "commit";
    private static final String ACTION_ENTER = "enter";
    private static final String ACTION_CURSOR = "cursor";

    // Server actions
    private static final String ACTION_CONNECTED = "connected";
    private static final String ACTION_DISCONNECTED = "disconnected";

    private static final boolean FORCE_RELOAD = true;
    private static final String TAG = AirKeysService.class.getSimpleName();
    private static final String WEBAPP_SOURCE = "root.war";
    private static final String WEBAPPS_DIR = "webapps";
    private static final String WEBAPP_NAME = "root.war";


    private TextView mMessage;
    private Button mButton;
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
            Log.v(TAG, "Exception stopping Jetty", e);
        } catch (Error e) {
            Log.v(TAG, "Error stopping Jetty", e);
        }
    }

    @Override
    public View onCreateInputView() {
        ViewGroup vg = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.keyboard, null);
        mMessage = (TextView) vg.findViewById(R.id.keyboard_text);
        mButton = (Button) vg.findViewById(R.id.keyboard_show_mehtods);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToNextIME();
            }
        });
        mButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showSelectInputMethodDialog(AirKeysService.this);
                return true;
            }
        });
        updateTextViewInfoClients();
        return vg;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "OnDestroy");
        super.onDestroy();
        stopServer();
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

        public void send(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mConnection.sendMessage(message);
                    } catch (Exception e) {
                        Log.d(TAG, "Failed to send message to a client");
                    }
                }
            }).start();
        }

        @Override
        public void onOpen(Connection connection) {
            Log.d(TAG, "OnOpen");
            mConnection = connection;
            mClients.add(this);
            updateTextViewInfoClients();
        }

        @Override
        public void onClose(int i, String s) {
            Log.d(TAG, "OnClose");
            mClients.remove(this);
            updateTextViewInfoClients();
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
        String message = new Gson().toJson(m);
        Log.d(TAG, "Sending: " + message);
        for (WSClient c : mClients) {
            c.send(message);
        }

    }

    class ClientMessage {
        String action;
        String text;
        int[] cursor;
    }

    class ServerMessage {
        String action;
        String text;
    }


    @SuppressLint("NewApi")
    private void switchToNextIME() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            final IBinder token = this.getWindow().getWindow().getAttributes().token;
            imm.switchToNextInputMethod(token, false);
        } else {
            showSelectInputMethodDialog(this);
        }
    }

    public static void showSelectInputMethodDialog(Context c) {
        InputMethodManager im = (InputMethodManager) c.getSystemService(INPUT_METHOD_SERVICE);
        im.showInputMethodPicker();
    }

    private void updateTextViewInfoClients() {
        if (mMessage == null) return;
        mMessage.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Setting text");
                synchronized (mClients) {
                    if (mClients.size() > 0) {
                        mMessage.setText("Connected to " + mClients.get(0).host);
                    } else {
                        mMessage.setText("Listening on " + NetUtil.getIPV4NetworkInterface() + ":" + HTTP_PORT);
                    }
                }
            }
        });
    }

    private String getCurrentText() {
        try {
            return (String) getCurrentInputConnection().getExtractedText(new ExtractedTextRequest(), 0).text;
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    private void onMessage(WSClient client, ClientMessage m) {
        InputConnection conn = getCurrentInputConnection();

        if (conn == null) Log.w(TAG, "InputConnection null");
        synchronized (conn) {
            switch (m.action) {
                case ACTION_TEXT:
                    int size = getCurrentText().length();
                    conn.deleteSurroundingText(size, size);
                    if (m.text != null)
                        conn.commitText(m.text, 1);
                    if (m.cursor != null) {
                        conn.setSelection(m.cursor[0], m.cursor[1]);
                    }
                    break;
                case ACTION_COMMIT:
                    conn.commitText(m.text, 1);
                    break;
                case ACTION_ENTER:
                    KeyEvent enterDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER);
                    conn.sendKeyEvent(enterDown);
                    KeyEvent enterUp = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER);
                    conn.sendKeyEvent(enterUp);
                    break;
                case ACTION_CURSOR:
                    conn.setSelection(m.cursor[0], m.cursor[1]);
                    break;
//            case ACTION_BACKSPACE:
//                conn.deleteSurroundingText(1, 0);
//                break;
//            case ACTION_DELETE:
//                conn.deleteSurroundingText(0, 1);
//                break;
            }
        }
    }
}
