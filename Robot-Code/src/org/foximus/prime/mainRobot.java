/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.foximus.prime;

import com.sun.squawk.util.MathUtils;
import edu.wpi.first.wpilibj.AnalogChannel;
import edu.wpi.first.wpilibj.DriverStationLCD;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class mainRobot extends IterativeRobot {
    
    double THETA = Math.toRadians(48);  //48 degrees as radians.  Current shooter angle.
    double G = 9.81;        //g: the gravitational acceleration—usually taken to be 9.81 m/s2 near the Earth's surface
                            //http://en.wikipedia.org/wiki/Trajectory_of_a_projectile#Notation
    
    double LOWBASKETY = .7112;
    double MIDBASKETY = 1.549;
    double TOPBASKETY = 2.489;
    
    double YTRIM = 0;
    double YOFFSET = -1.003/*shooter height*/ + .1524/*center backthing*/;
    double XTRIM = .8;
    double XOFFSET = 0;
    double calced = 0;
    boolean trimStop = false;
    double armspeed = .25;
    
    boolean recalc = true;
    boolean calib = false;
    
    AnalogChannel ultrasonic = new AnalogChannel(1);
    AnalogChannel potentiameter = new AnalogChannel(2);
    
    Relay botPickup = new Relay(1);
    Relay topPickup = new Relay(2);
    
    Victor shooterT = new Victor(8);
    Victor shooterB = new Victor(9);
    
    Victor arm = new Victor(5);
    Victor shootRot = new Victor(6);

    Joystick joy1 = new Joystick(1);
    Joystick joy2 = new Joystick(2);
    
    RobotDrive drive = new RobotDrive(3,4);
    
    
    public double getXDistance(){ //currently meters.  METRIC!
        
        double d = ultrasonic.getVoltage() / 0.009766;
        
        d *= 0.0254; 
        
        return d;
    }
    public double v(double x, double y, double theta, double g){
        
        double v;
        
        v= -4 * ( 2*g*y - 2*x*g*Math.tan(theta)) * (g*g*x*x+g*g*x*x*Math.tan(theta)*Math.tan(theta));
        
        if(v < 0){
            return -1;}
        
        v = Math.sqrt(v) / (2 *(2*y*g - 2*g*x*Math.tan(theta)));
        
        if(v<0){
            v = -v;}
        
        return v;
    }
    public double speedToPower(double speed){//meterspersecond.  Based on exponential regression and experiemental results.
        return .08817160176329* MathUtils.pow(1.2861446832546,speed);
    }
    public double calcShooterPower(double basket){
        double r = speedToPower(v(getXDistance()+XOFFSET+XTRIM, basket + YOFFSET + YTRIM,THETA, G));
        
        if(r > 1){
            r = 1;}
        else if(r < 0){
            r = 0;}
        return r;
    }
    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
        getWatchdog().setEnabled(false);
        botPickup.setDirection(Relay.Direction.kReverse);
        topPickup.setDirection(Relay.Direction.kForward);
        trimStop = false;
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
        Timer timer = new Timer();
        
        timer.start();
        
        int step = 0;
        double tim = timer.get() + 2.5;
        double tim2 = timer.get() + 5;
        
         while (true && isAutonomous() && isEnabled() ){             
               
                shooterT.set(calcShooterPower(MIDBASKETY));
                shooterB.set(calcShooterPower(MIDBASKETY));   
                botPickup.set(Relay.Value.kOn);
                if(timer.get() > tim && timer.get() < tim2){
                    topPickup.set(Relay.Value.kOn);
                }
                else if (timer.get() > tim){
                    tim += 5;
                    tim2 += 5;                       
                    topPickup.set(Relay.Value.kOff);
                }
                else{
                    topPickup.set(Relay.Value.kOff);}
                
         }
    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
        //double magnitude, direction, rotation;
        //Create "deadzone" variables. Adjust threshold value to increase/decrease deadzone
        //double X2 = 0, Y1 = 0, X1 = 0, threshold = 15.0;
        
        Timer timer = new Timer();
        
        timer.start();
    
        double selectedBasket = LOWBASKETY;
        double trimStop = timer.get();
            
        while (true && isOperatorControl() && isEnabled()) // loop until change 
        {
            
            if(!joy1.getRawButton(8)){                
                botPickup.setDirection(Relay.Direction.kReverse);
                topPickup.setDirection(Relay.Direction.kForward);
            }
            else{
                botPickup.setDirection(Relay.Direction.kForward);     
                topPickup.setDirection(Relay.Direction.kReverse);           
            }
            
            if(!calib){
            if(joy1.getRawButton(10)){
                drive.arcadeDrive(.5*joy1.getY(), -.5 * joy1.getX());}
            else if(joy1.getRawButton(11)){
                drive.arcadeDrive(.7*joy1.getY(), -.7 * joy1.getX());}
            else{
                drive.arcadeDrive(joy1.getY(), -joy1.getX());}
            
            if(trimStop < timer.get() && joy2.getRawButton(6)){
                trimStop = timer.get() + .1;
                XTRIM = XTRIM + .1;
            }
            else if(trimStop < timer.get() && joy2.getRawButton(7)){
                trimStop = timer.get() + .1;
                XTRIM = XTRIM - .1;
            }
            else if(trimStop < timer.get() && (joy2.getRawButton(11) || joy1.getRawButton(6))){
                trimStop = timer.get() + .1;
                armspeed = armspeed + .025;
            }
            else if(trimStop < timer.get() &&( joy2.getRawButton(10)  || joy1.getRawButton(7))){
                trimStop = timer.get() + .1;
                armspeed = armspeed - .025;
            }
            
            if(-joy2.getY() > 0.0){
                shooterT.set(-joy2.getY());
                shooterB.set(-joy2.getY());
            } else if (joy2.getRawButton(4)) {
                if(recalc){
                    calced = calcShooterPower(LOWBASKETY);
                    recalc = false;
                }
                shooterT.set(calced);
                shooterB.set(calced);   
                selectedBasket = LOWBASKETY;
            } else if (joy2.getRawButton(3)) {
                if(recalc){
                    calced = calcShooterPower(MIDBASKETY);
                    recalc = false;
                }
                shooterT.set(calced);
                shooterB.set(calced);        
                selectedBasket = MIDBASKETY;
            } else if (joy2.getRawButton(5)) {
                if(recalc){
                    calced = calcShooterPower(TOPBASKETY);
                    recalc = false;
                }
                shooterT.set(calced);
                shooterB.set(calced);    
                selectedBasket = TOPBASKETY;
            } 
            else if (joy1.getRawButton(9)) {
                calced = 0;
                recalc = true;
                shooterT.set(calced);
                shooterB.set(calced);    
            } else {
                calced = 0;
                recalc = true;
                shooterT.set(calced);
                shooterB.set(calced);
            }
            if(joy1.getTrigger() || joy2.getRawButton(2)){
                botPickup.set(Relay.Value.kOn);}
            else{
                botPickup.set(Relay.Value.kOff);}
            
            if(joy1.getRawButton(3) || joy2.getRawButton(8)){
                arm.set(.25);}
            else if(joy1.getRawButton(2) || joy2.getRawButton(9)){
                arm.set(-armspeed);}
            else {
                arm.stopMotor();}
            
            
            shootRot.set(-.35*joy2.getX());
                        
            if(joy2.getTrigger()){
                topPickup.set(Relay.Value.kOn);
            //else if(joy2.getRawButton(3))
              //  botPickup.set(1.0);
            }
                else{
                topPickup.set(Relay.Value.kOff);}
        
            DriverStationLCD.getInstance().println(DriverStationLCD.Line.kUser2, 1, " oo  Shooter"+ calced); //joy2.getAxis(Joystick.AxisType.kThrottle));
            DriverStationLCD.getInstance().println(DriverStationLCD.Line.kUser3, 1, "<|>  Sonic:"+getXDistance());
            DriverStationLCD.getInstance().println(DriverStationLCD.Line.kUser4, 1, "_|/  XTRIM: "+  XTRIM);
            DriverStationLCD.getInstance().println(DriverStationLCD.Line.kUser5, 1, "  armspeed: "+  armspeed);
            DriverStationLCD.getInstance().updateLCD();
            }
            else {
                
                int foo = 2+2; //foo is 4
                
                if(joy1.getRawButton(2)){
                    shooterT.set(calcShooterPower(.25));
                    shooterB.set(calcShooterPower(.25));
                }
                else if(joy1.getRawButton(3)){
                    shooterT.set(calcShooterPower(.35));
                    shooterB.set(calcShooterPower(.35));
                }
                else if(joy1.getRawButton(4)){
                    shooterT.set(calcShooterPower(.50));
                    shooterB.set(calcShooterPower(.50));
                }
                else if(joy1.getRawButton(5)){
                    shooterT.set(calcShooterPower(.65));
                    shooterB.set(calcShooterPower(.65));
                }
                else if(joy1.getRawButton(6)){
                    shooterT.set(calcShooterPower(.75));
                    shooterB.set(calcShooterPower(.75));
                }
                else if(joy1.getRawButton(7)){
                    shooterT.set(calcShooterPower(.85));
                    shooterB.set(calcShooterPower(.85));
                }
                else if(joy1.getRawButton(8)){
                    shooterT.set(calcShooterPower(.1));
                    shooterB.set(calcShooterPower(.1));
                }
                else{
                    shooterT.set(0);
                    shooterB.set(0);}
            }
            
                DriverStationLCD.getInstance().println(DriverStationLCD.Line.kUser2, 1, "Our team is good lookin'!!1!");
           
            
            if(joy1.getTrigger() || joy2.getRawButton(2)){
                botPickup.set(Relay.Value.kOn);}
            else{
                botPickup.set(Relay.Value.kOff);
            }
            
            if(joy2.getTrigger()){
                topPickup.set(Relay.Value.kOn);}
            //else if(joy2.getRawButton(3))
              //  botPickup.set(1.0);
            else{
                topPickup.set(Relay.Value.kOff);}
    
            
            
            
            //drive.tankDrive(joy1, joy2);
            /*
            if(joy1.getTrigger(Hand.kLeft)) {
                shooterT.set(joy1.getThrottle());
                shooterB.set(joy1.getThrottle());
            } else { shooterT.set(0); shooterB.set(0); }
            if(joy1.getRawButton(6)) {
                secondLift.set(joy2.getThrottle());
            } else {secondLift.set(0);}
            if(joy1.getRawButton(7)) {
                firstLift.set(joy2.getThrottle());
            } else { firstLift.set(0); }
            if(joy1.getRawButton(8)) {
                shooterRot.set(.1);
            } else { shooterRot.set(0); }
            if(joy1.getRawButton(9)) {
                shooterRot.set(-.1);
            } else { shooterRot.set(0); }
            */
            /*magnitude = joy1.getMagnitude();
            direction = joy1.getDirectionDegrees();
            rotation = joy2.getX();*/
            //drive.mecanumDrive_Cartesian(magnitude, direction, rotation, 0); 
            //drive.mecanumDrive_Cartesian(joy1.getX(), joy1.getY(), joy1.getZ(), 0.0);
            //drive.mecanumDrive_Cartesian(joy1.getX(), joy1.getY(), joy2.getX(), 180.0);
            //Timer.delay(0.005);
            
            
            // ... or for two 2-axis joysticks do this (Halo):
            /*double forward = -joy1.getY(); // push joystick1 forward to go forward
            double right = joy1.getX(); // push joystick1 to the right to strafe right
            double clockwise = joy2.getX(); // push joystick2 to the right to rotate clockwise
            
            DriverStationLCD.getInstance().println(DriverStationLCD.Line.kUser2, 1, "Joy1 X:"+right+" Y:"+forward);
            DriverStationLCD.getInstance().println(DriverStationLCD.Line.kUser4, 1, "Joy2 X:"+clockwise);
            DriverStationLCD.getInstance().updateLCD();
            
            // note: the above can only do 2 degrees of freedom at a time.
            // Button1 selects Halo or Arcade.
            // ... or any other driver interface scheme you like.
            // K is a tuning constant for the �rotate� axis sensitivity.
            // Start with K=0, and increase it very slowly (do not exceed K=1)
            // to find the right value after you�ve got fwd/rev and strafe working:
            double K = 0.1;
            clockwise = K*clockwise;
            // OPTIONAL. If desired, use the gyro angle for field-centric control.
            // "theta" is the gyro angle, measured CCW from the zero reference:
            double temp = forward*cos(theta) - right*sin(theta);
            right = forward*sin(theta) + right*cos(theta);
            forward = temp;
            // Now apply the inverse kinematic tranformation
            // to convert your vehicle motion command
            // to 4 wheel speed commands:
            double front_left = forward + clockwise + right;
            double front_right = forward - clockwise - right;
            double rear_left = forward + clockwise - right;
            double rear_right = forward - clockwise + right;
            // Finally, normalize the wheel speed commands
            // so that no wheel speed command exceeds magnitude of 1:
            double max = Math.abs(front_left);
            if (Math.abs(front_right)>max) max = Math.abs(front_right);
            if (Math.abs(rear_left)>max) max = Math.abs(rear_left);
            if (Math.abs(rear_right)>max) max = Math.abs(rear_right);
            if (max>1)
            {front_left/=max; front_right/=max; rear_left/=max; rear_right/=max;}
            // You're done. Send these four wheel commands to their respective wheels
            FL.set(front_left);
            BL.set(rear_left);
            FR.set(front_right);
            BR.set(rear_right);
            */
            
            /*
            magnitude = joy1.getY();
            direction = joy1.getX();
            rotation = joy2.getX();
            
            drive.mecanumDrive_Polar(magnitude, direction, rotation);
            Timer.delay(0.005);
            */

            //Create "deadzone" for Y1
            /*Y1 = joy1.getY();
            X1 = joy1.getX();
            X2 = joy2.getX();*/
            /*
            if(Math.abs(joy1.getY()) > threshold)
                Y1 = joy1.getY();
            else
                Y1 = 0;
            //Create "deadzone" for X1
            if(Math.abs(joy1.getX()) > threshold)
                X1 = joy1.getX();
            else
                X1 = 0;
            //Create "deadzone" for X2
            if(Math.abs(joy2.getX()) > threshold)
                X2 = joy2.getX();
            else
                X2 = 0;
            *//*
            double max = 0.0;
            if(Math.abs(Y1 - X2 - X1) > max)
                max = Math.abs(Y1 - X2 - X1);
            if(Math.abs(Y1 - X2 + X1) > max)
                max = Math.abs(Y1 - X2 + X1);
            if(Math.abs(Y1 + X2 + X1) > max)
                max = Math.abs(Y1 + X2 + X1);
            if(Math.abs(Y1 + X2 - X1) > max)
                max = Math.abs(Y1 + X2 - X1);

            //Remote Control Commands
            FR.set((Y1 - X2 - X1)/max);
            BR.set((Y1 - X2 + X1)/max);
            FL.set((Y1 + X2 + X1)/max);
            BL.set((Y1 + X2 - X1)/max);
             */
        }

    }    
}
