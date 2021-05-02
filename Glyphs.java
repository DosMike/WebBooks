import java.io.*;

public class Glyphs {
    public static void main(String[] args) { try {
        InputStream in = new FileInputStream(new File("glyph_sizes.bin"));
        File outdir = new File("src/main/resources/assets/webbook");
        outdir.mkdirs();
        OutputStream out = new FileOutputStream(new File(outdir, "glyph_width.bin"));
        int c,l,r,i=0;
        while ((c=in.read())>=0) {
            l = ((c&0xF0)>>4);
            r = (c&0x0F);
            l = i==32?3:(r-l+1); //space is not 0-16 wide!
            out.write((l<=0?3:l)&0xFF);
            i++;
        }
        out.flush();
        out.close();
        in.close();
    } catch(Throwable e) { e.printStackTrace(); } }
}