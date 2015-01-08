package org.twinone.airkeys;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by twinone on 1/8/15.
 */
public class NetUtil {

    public static String getIPV4NetworkInterface() {
        List<String> ifaces = getIPv4NetworkInterfaces();
        return (ifaces.size() > 0) ? ifaces.get(0) : null;
    }

    public static boolean isValidIPv4Address(String ip) {
        if (ip == null || ip.equals(""))
            return false;
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15))
            return false;

        try {
            Pattern pattern = Pattern
                    .compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    public static List<String> getIPv4NetworkInterfaces() {
        List<String> ret = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface
                    .getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nis)) {
                Enumeration<InetAddress> iis = ni.getInetAddresses();
                for (InetAddress ia : Collections.list(iis)) {
                    String addr = ia.getHostAddress();
                    if (isValidIPv4Address(addr) && !addr.equals("127.0.0.1")) {
                        ret.add(ia.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
        }
        return ret;
    }
}
