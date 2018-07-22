/*
 *  Copyright (C) 2000-2004 Roger Butenuth, Andreas Gampe,
 *                          Stefan Goetz, Sebastian Pappert,
 *                          Klaas Prause, Enno Rehling,
 *                          Sebastian Tusk, Ulrich Kuester,
 *                          Ilja Pavkovic
 *
 * This file is part of the Eressea Java Code Base, see the
 * file LICENSING for the licensing information applying to
 * this file.
 *
 */

package com.fftools.utils;

import magellan.library.CoordinateID;

/**
 * A CoordinateID uniquely identifies a location in a three dimensional space by x-, y- and z-axis
 * components. This is an immutable object.
 */
public final class FFToolsCoordinateID  {

  /**
   * The x-axis part of this CoordinateID. Modifying the x, y and z values changes the hash value of
   * this CoordinateID!
   */
  private int x;

  /**
   * The y-axis part of this CoordinateID. Modifying the x, y and z values changes the hash value of
   * this CoordinateID!
   */
  private int y;

  /**
   * The z-axis part of this CoordinateID. Modifying the x, y and z values changes the hash value of
   * this CoordinateID!
   */
  private int z;

  

  /**
   * Creates a new CoordinateID object.
   */
  private FFToolsCoordinateID(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * Creates a new CoordinateID object.
   */
  private FFToolsCoordinateID(CoordinateID c) {
    x = c.getX();
    y = c.getY();
    z = c.getZ();
  }

  /**
   * Returns the value of x.
   * 
   * @return Returns x.
   */
  public int getX() {
    return x;
  }

  /**
   * Returns the value of y.
   * 
   * @return Returns y.
   */
  public int getY() {
    return y;
  }

  /**
   * Returns the value of z.
   * 
   * @return Returns z.
   */
  public int getZ() {
    return z;
  }

  

  /**
   * Two instances are equal if their x,y,z values are equal.
   */
  @Override
  public boolean equals(Object o) {
    // return this == o;
    if (o == this)
      return true;
   
    if (o instanceof FFToolsCoordinateID) {
    	FFToolsCoordinateID c = (FFToolsCoordinateID) o;

      return (x == c.x) && (y == c.y) && (z == c.z);
    }
    return false;
  }

 
  /**
   * Creates a new CoordinateID object.
   */
  public static FFToolsCoordinateID create(int x, int y, int z) {
    return new FFToolsCoordinateID(x, y, z);
  }
  
  /**
   * Creates a new CoordinateID object.
   */
  public static FFToolsCoordinateID create(CoordinateID c) {
    return new FFToolsCoordinateID(c.getX(), c.getY(), c.getZ());
  }


  /**
   * Return a new CoordinateID that is this one modified by c.x on the x-axis and c.y on the y-axis
   * and c.z on the z-axis.
   * 
   * @param c the relative CoordinateID to translate the current one by.
   * @return A new CoordinateID
   */
  public FFToolsCoordinateID translate(CoordinateID c) {
   
    return create(x + c.getX(), y + c.getY(), z + c.getZ());
  }

 
  public void setToCoordinateID(CoordinateID c){
	  this.x = c.getX();
	  this.y = c.getY();
	  this.z = c.getZ();
  }

  
  /**
   * Returns a String representation of this coordinate. The x, y and z components are seperated by
   * semicolon with a blank and the z component is ommitted if it equals 0.
   */
  @Override
  public String toString() {
    return toString(", ", false);
  }

  /**
   * Returns a String representation of this CoordinateID consisting of the x, y and, if not 0, z
   * coordinates delimited by delim.
   */
  public String toString(String delim) {
    return toString(delim, false);
  }

  /**
   * Returns a String representation of this CoordinateID. The x, y and z components are seperated
   * by the specified string and the z component is ommitted if it equals 0 and forceZ is false.
   * 
   * @param delim the string to delimit the x, y and z components.
   * @param forceZ if true, the z component is only included if it is not 0, else the z component is
   *          always included.
   */
  public String toString(String delim, boolean forceZ) {
    if (!forceZ && (z == 0))
      return x + delim + y;
    else
      return x + delim + y + delim + z;
  }

}
