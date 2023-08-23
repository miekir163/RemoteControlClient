package com.itant.rt.utils;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 压缩流程：
 * 原流程：--> encrypt --> base64 encode --> compress --> make QR code
 * 新流程：--> compress --> encrypt --> make QR code
 */
public final class ZipUtils {
    private ZipUtils() {}

    /**
     * jzlib 压缩数据
     * <a href="http://www.jcraft.com/jzlib/">jzlib</a>
     * @param object
     * @return
     * @throws IOException
     */
    public static byte[] jzlib(byte[] object) {

        byte[] data = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZOutputStream zOut = new ZOutputStream(out, JZlib.Z_DEFAULT_COMPRESSION);
            DataOutputStream objOut = new DataOutputStream(zOut);
            objOut.write(object);
            objOut.flush();
            zOut.close();
            data = out.toByteArray();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * jzLib压缩的数据
     *
     * @param object
     * @return
     * @throws IOException
     */
    public static byte[] unJzlib(byte[] object) {

        byte[] data = null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(object);
            ZInputStream zIn = new ZInputStream(in);
            byte[] buf = new byte[1024];
            int num = -1;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((num = zIn.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, num);
            }
            data = baos.toByteArray();
            baos.flush();
            baos.close();
            zIn.close();
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}
