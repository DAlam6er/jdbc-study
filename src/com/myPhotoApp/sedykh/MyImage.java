package com.myPhotoApp.sedykh;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class MyImage extends JComponent {
  private BufferedImage buf;

  public void setImage(InputStream inputStream) {
    try {
      buf = ImageIO.read(inputStream);
      if (buf != null) repaint();
    } catch (IOException e) {
      System.err.println("Buffer is empty: " + e.getMessage());
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (buf != null) {
      double k = (double) getWidth() / buf.getWidth();
      int h = (int) (buf.getHeight() * k);
      Image image = buf.getScaledInstance(getWidth(), h, Image.SCALE_FAST);

      if (image != null) {
        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(image, 0, 0, null);
      }
    } else g.drawString("No image available", 20, 20);
  }
}
