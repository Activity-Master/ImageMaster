package com.guicedee.activitymaster.imagemaster.services;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

public class GraphicsUtil
{
	protected Graphics g;
	protected ImageObserver observer;
	public GraphicsUtil(Graphics g, ImageObserver observer)
	{
		this.g = g;
		this.observer = observer;
	}
	
	public enum Align
	{
		North, NorthEast, East, SouthEast, South, SouthWest, West, NorthWest, Center
	}
	
	public void drawCentered(BufferedImage img, Point2D location)
	{
		drawCentered(g, img, location, img.getWidth(), img.getHeight(), observer);
	}
	public void drawCentered(BufferedImage img, Point2D location, double newWidth, double newHeight)
	{
		drawCentered(g, img, location, newWidth, newHeight, observer);
	}
	public static void drawCentered(Graphics g, BufferedImage img, Point2D location)
	{
		drawCentered(g, img, location, img.getWidth(), img.getHeight(), null);
	}
	public static void drawCentered(Graphics g, BufferedImage img, Point2D location, ImageObserver observer)
	{
		drawCentered(g, img, location, img.getWidth(), img.getHeight(), observer);
	}
	public static void drawCentered(Graphics g, BufferedImage img, Point2D location, double newWidth, double newHeight)
	{
		drawCentered(g, img, location, newWidth, newHeight, null);
	}
	public static void drawCentered(Graphics g, BufferedImage img, Point2D location, double newWidth, double newHeight, ImageObserver observer)
	{
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage(
				img,                  // what to draw
				(int)(location.getX() - (newWidth/2)),  // dest left
				(int)(location.getY() - (newHeight/2)),  // dest top
				(int)(location.getX() + (newWidth/2)),  // dest right
				(int)(location.getY() + (newHeight/2)),  // dest bottom
				0,                    // src left
				0,                    // src top
				img.getWidth(),              // src right
				img.getHeight(),            // src bottom
				observer                // to notify of image updates
		);
	}
}