import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Generates the launcher icons (photo-album motif) at all densities. */
public class GenIcon {
    public static void main(String[] a) throws Exception {
        int[] sizes = {48, 72, 96, 144, 192};
        String[] dirs = {"mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"};
        String base = a.length > 0 ? a[0] : ".";
        for (int i = 0; i < sizes.length; i++) {
            int s = sizes[i];
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // rounded purple gradient background
            g.setPaint(new GradientPaint(0, 0, new Color(0x7B1FA2), s, s, new Color(0xEC407A)));
            g.fill(new RoundRectangle2D.Float(0, 0, s, s, s * 0.22f, s * 0.22f));
            // album "page" white card
            float m = s * 0.20f;
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(m, m, s - 2 * m, s - 2 * m, s * 0.06f, s * 0.06f));
            // photo (mountain + sun) inside
            float pm = s * 0.28f;
            Shape clip = new RoundRectangle2D.Float(pm, pm, s - 2 * pm, s - 2 * pm, s * 0.04f, s * 0.04f);
            g.setColor(new Color(0xBBDEFB));
            g.fill(clip);
            Shape old = g.getClip();
            g.setClip(clip);
            g.setColor(new Color(0xFFB300));
            float sun = s * 0.09f;
            g.fill(new Ellipse2D.Float(s * 0.58f, pm + s * 0.03f, sun, sun));
            g.setColor(new Color(0x66BB6A));
            Polygon mtn = new Polygon();
            mtn.addPoint((int) pm, (int) (s - pm));
            mtn.addPoint((int) (s * 0.45f), (int) (s * 0.5f));
            mtn.addPoint((int) (s * 0.72f), (int) (s - pm));
            g.fillPolygon(mtn);
            g.setColor(new Color(0x43A047));
            Polygon mtn2 = new Polygon();
            mtn2.addPoint((int) (s * 0.5f), (int) (s - pm));
            mtn2.addPoint((int) (s * 0.68f), (int) (s * 0.58f));
            mtn2.addPoint((int) (s - pm), (int) (s - pm));
            g.fillPolygon(mtn2);
            g.setClip(old);
            g.dispose();
            File dir = new File(base, "res/" + dirs[i]);
            dir.mkdirs();
            ImageIO.write(img, "png", new File(dir, "ic_launcher.png"));
        }
        System.out.println("icons generated");
    }
}
