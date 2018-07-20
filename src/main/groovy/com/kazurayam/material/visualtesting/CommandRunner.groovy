package com.kazurayam.material.visualtesting

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CommandRunner {
    
    static Logger logger_ = LoggerFactory.getLogger(CommandRunner.class)

    private CommandRunner() {}

    /**
     * Run a OS-level command from Java.
     *
     * Learned how to at https://qiita.com/itoa06/items/9e761d53c58eeb20490e
     *
     * @param args
     * @param out
     * @return returns the valued returned by ImageMagick
     */
    static int runCommand(String[] args, OutputStream out, OutputStream err) {
        try {
            Process process = new ProcessBuilder(args).start()

            // 外部プロセスは標準出力のバッファと標準エラー出力のバッファをもっている。バッファに中身が残っていると
            // waitFor()が返ってこない。waitForする前にバッファから中身を吸い取る必要がある。
            // 標準出力と標準エラー出力とどちらに中身が残っているか不定なので順序を前提することができない。
            // だから外部プロセスの標準出力と標準エラー出力を別スレッドでsqueezeする。
            // https://qiita.com/shintaness/items/6dd91260726e555c49e5
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    transfer(squeeze(process.getErrorStream()), err)
                }
            })
            th.start()
            transfer(squeeze(process.getInputStream()), out)

            int ret = process.waitFor()
            return ret
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace()
            return -1
        }
    }

    /**
     * squeeze the output from the given InputStream, return it as a String
     *
     * @param is
     * @return
     */
    private static String squeeze(InputStream is) {
        InputStreamReader isr = new InputStreamReader(is, System.getProperty("file.encoding"))
        BufferedReader reader = new BufferedReader(isr)
        StringBuilder sb = new StringBuilder()
        int c
        while ((c = reader.read()) != -1) {
            sb.append((char)c)
        }
        is.close()
        return sb.toString()
    }

    private static void transfer(String str, OutputStream os) {
        OutputStreamWriter osw = new OutputStreamWriter(os, System.getProperty("file.encoding"))
        BufferedWriter writer = new BufferedWriter(osw)
        writer.write(str)
        writer.flush()
        writer.close()
    }

}