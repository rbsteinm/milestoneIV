package cs211.boardgame;
import java.util.ArrayList;
import java.util.List;
import processing.core.*;
import processing.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

import cs211.boardgame.DigitalGame.HScrollbar;
import cs211.imageprocessing.ImageProcessing;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

/**
 * @author rbsteinm
 *
 */
@SuppressWarnings("serial")
public class TangibleGame extends PApplet{
	
	//TODO LA VITESSE N'EST PAS NULLE LORSQUE LA BALLE EST IMMOBILE, CE QUI
	//POSE PROBLEME LORS DU CALCUL DU SCORE. A REGLER RAPIDEMENT
	//Solution: poser un treshhold t: si v < t => v = 0
	
	private final static int MIN_WHEEL_VALUE = 0;
	private final static int MAX_WHEEL_VALUE = 100;
	private final static float MIN_ANGLE = -(PI/3);
	private final static float MAX_ANGLE = (PI/3);
	private final static float SPHERE_RADIUS = 10.0f;
	private final static float PLATE_WIDTH = 350.0f;
	private final static float PLATE_DEPTH = 350.0f;
	private final static float PLATE_HEIGHT = 20.0f;
	private final static float CYLINDER_HEIGHT = 40.0f;
	private final static float CYLINDER_RADIUS = 15.0f;
	private final static int CYLINDER_RESOLUTION = 40;
	private final static int DATA_SURFACE_HEIGHT = 140;
	private final static int TOP_VIEW_PLATE_WIDTH = 100;
	private final static int TOP_VIEW_PLATE_HEIGHT = 100;
	private final static int DATA_SURFACE_MARGIN = 10;
	private final static int SCOREBOARD_WIDTH = 100;
	private final static int SCOREBOARD_HEIGHT = 100;
	private final static int BARCHART_WIDTH = 1000;
	private final static int BARCHART_HEIGHT = 100;
	private final static int SCROLLBAR_HEIGHT = 10;
	private final static int SCROLLBAR_WIDTH = 400;
	private final static int MAX_SCORE = 1000;
	private final static int MIN_SCORE = 0;
	private final static float DEFAULT_BARCHART_RECT_WIDTH = 3.0f;
	private final static int PIN_HIT_BONUS = 50;
	private final static float VELOCITY_TRESHOLD = 0.5f;
	
	private float barchartRectWidth = DEFAULT_BARCHART_RECT_WIDTH;
	private float barchartRectHeight = DEFAULT_BARCHART_RECT_WIDTH;
	private float barchartRectMargin = DEFAULT_BARCHART_RECT_WIDTH / 3.0f;
	
	private HScrollbar scrollbar;
	
	private TimerTask task;
	private Timer timer;
	
	private PGraphics dataSurface;
	private PGraphics topView;
	private PGraphics scoreboard;
	private PGraphics barChart;
	
	private List<Float> scores = new ArrayList<Float>();
	
	private float rotateX = 0.0f;
	private float rotateY = 0.0f;
	private float rotateZ = 0.0f;
	private float rotateSpeedXZ = 350.0f;
	private float rotateSpeedY = PI/6;
	private float boardRotateSpeed = 2.0f;
	
	private float wheelValue = 50.0f;
	
	private boolean shiftView = false;
	
	private Mover ball = new Mover();
	private PShape bowlingPin;
	private List<PVector> bowlingPins = new ArrayList<PVector>();
	private List<Integer> pinsToBeRemoved = new ArrayList<Integer>();
	private float score = 0;
	private float lastScore = 0;
	
	private ImageProcessing imageProcessing;
	private boolean quadDetected;
	
	PImage backgroundImage;
	Minim audioContext;
	AudioPlayer backgroundPlayer;
	
	public void setup(){
		size(1500, 800, P3D);
		size(1400, 800, P3D);
		audioSetup();
		graphicSetup();
		setupTimer();
		imageProcessing = new ImageProcessing(this);
		imageProcessing.setup();
	}
	
	public void graphicSetup(){
		bowlingPin = loadShape("bowlingPin7.obj");
		dataSurface = createGraphics(width, DATA_SURFACE_HEIGHT, P2D);
		topView = createGraphics(TOP_VIEW_PLATE_WIDTH, TOP_VIEW_PLATE_HEIGHT, P2D);
		scoreboard = createGraphics(SCOREBOARD_WIDTH + 2*DATA_SURFACE_MARGIN, DATA_SURFACE_HEIGHT);
		barChart = createGraphics(width - TOP_VIEW_PLATE_WIDTH - SCOREBOARD_WIDTH - 4*DATA_SURFACE_MARGIN, DATA_SURFACE_HEIGHT);
		scrollbar = new HScrollbar(3*DATA_SURFACE_MARGIN + TOP_VIEW_PLATE_WIDTH + SCOREBOARD_WIDTH, height - DATA_SURFACE_MARGIN - SCROLLBAR_HEIGHT, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
	}
	
	public void audioSetup(){
		audioContext = new Minim(this);
		backgroundPlayer = audioContext.loadFile("ScottJoplinEuphonicSounds.mp3");
		backgroundPlayer.loop();
	}
	
	public void setupTimer(){
		task = new TimerTask()
		{
			@Override
			public void run() 
			{
				scores.add(score);
			}	
		};
		
		timer = new Timer();
		timer.scheduleAtFixedRate(task, 0, 1000);
	}
	
	public void draw(){
		lights();
		background(204, 229, 255);
		drawGame();
		noLights();
		drawBottomSurface();
		scrollbar.update();
		scrollbar.display();
		quadDetected = imageProcessing.draw();
		updateAnglesFromBoard();
	}
	
	public void drawGame(){
		pushMatrix();
		translate(width/2, height/2);
		if(!shiftView){
			rotateX(rotateX);
			rotateY(rotateY);
			rotateZ(rotateZ);
			if(quadDetected){
				fill(0, 255, 0);
			}
			else{
				fill(255, 0, 0);
			}
			box(PLATE_WIDTH, PLATE_HEIGHT, PLATE_DEPTH);
			noFill();
			stroke(0);
			for(PVector cylinderPosition: bowlingPins){
				displayBowlingPin(cylinderPosition.x, cylinderPosition.y);
				//cylinder.show(cylinderPosition.x, cylinderPosition.y);
			}
			noStroke();
			ball.show();
		}
		else{
			rect(-PLATE_WIDTH/2, -PLATE_DEPTH/2, PLATE_WIDTH, PLATE_DEPTH);
			ball.shiftModeShow();
			for(PVector cylinderPosition: bowlingPins){
				//showing pins positions with circles on the shiftMode plate
				stroke(0);
				ellipse(cylinderPosition.x, cylinderPosition.y, CYLINDER_RADIUS*2, CYLINDER_RADIUS*2);
				noStroke();
			}
		}
		popMatrix();
	}
	
	public void drawBottomSurface(){
		drawDataSurface();
		drawTopViewSurface();
		drawScoreboard();
		drawBarchart();
	}
	
	public void drawDataSurface() {
		dataSurface.beginDraw();
		dataSurface.background(255, 228, 140);
		dataSurface.endDraw();
		image(dataSurface, 0, height - DATA_SURFACE_HEIGHT);
	}
	
	public void drawTopViewSurface(){
		topView.beginDraw();
		topView.pushMatrix();
		topView.translate(topView.width/2, topView.height/2);
		topView.background(0, 0, 255);
		//displaying ball on topView surface
		topView.noStroke();
		topView.fill(255, 0, 0);
		float ballPosX = map(ball.location.x, -PLATE_WIDTH/2, PLATE_WIDTH/2, -topView.width/2, topView.width/2);
		float ballPosZ = map(ball.location.z, -PLATE_DEPTH/2, PLATE_DEPTH/2, -topView.height/2, topView.height/2);
		topView.ellipse(ballPosX, ballPosZ, 5, 5);
		//displaying cylinders on topView surface
		topView.fill(255);
		for(PVector cyl: bowlingPins){
			float cylX = map(cyl.x, -PLATE_WIDTH/2, PLATE_WIDTH/2, -topView.width/2, topView.width/2);
			float cylZ = map(cyl.y, -PLATE_DEPTH/2, PLATE_DEPTH/2, -topView.height/2, topView.height/2);
			topView.ellipse(cylX, cylZ, 10, 10);
		}
		topView.popMatrix();
		topView.endDraw();
		image(topView, DATA_SURFACE_MARGIN,  height - DATA_SURFACE_MARGIN - topView.width);
	}
	
	public void drawScoreboard(){
		scoreboard.beginDraw();
		scoreboard.stroke(255);
		scoreboard.strokeWeight(2);
		scoreboard.fill(255, 228, 140);
		scoreboard.rect(DATA_SURFACE_MARGIN, DATA_SURFACE_MARGIN, SCOREBOARD_WIDTH, SCOREBOARD_HEIGHT);
		noStroke();
		String scoreText = "";
		scoreText += "score:\n" + roundNumber(score);
		scoreText += "\nVelocity: \n" + roundNumber(ball.velocity.mag());
		scoreText += "\nLast score: \n" + roundNumber(lastScore);
		scoreboard.textSize(10);
		scoreboard.fill(0);
		scoreboard.text(scoreText, DATA_SURFACE_MARGIN*2, DATA_SURFACE_MARGIN*2);
		scoreboard.endDraw();
		image(scoreboard, TOP_VIEW_PLATE_WIDTH + DATA_SURFACE_MARGIN, height - scoreboard.height);
	}
	
	public void drawBarchart(){
		barChart.beginDraw();
		//draw background rectangle
		barChart.fill(255, 240, 160);
		barChart.stroke(255);
		barChart.strokeWeight(2);
		barChart.rect(DATA_SURFACE_MARGIN, DATA_SURFACE_MARGIN, BARCHART_WIDTH, BARCHART_HEIGHT);
		barChart.noFill();
		barChart.noStroke();
		//draw small score rectangles
		barChart.fill(0, 0, 255);
		barchartRectWidth = 2*scrollbar.getPos()*DEFAULT_BARCHART_RECT_WIDTH;
		for(int i = 0; i < scores.size(); i++){
			if(DATA_SURFACE_MARGIN + barchartRectMargin + i *(barchartRectWidth + barchartRectMargin) < BARCHART_WIDTH){
				for(int j = 0; 50*j < scores.get(i); j++){
					barChart.rect(DATA_SURFACE_MARGIN + barchartRectMargin + i *(barchartRectWidth + barchartRectMargin),
							BARCHART_HEIGHT - j*(barchartRectHeight + barchartRectMargin),
							barchartRectWidth,
							barchartRectHeight);
				}
			}
		}
		barChart.noFill();
		barChart.endDraw();
		image(barChart, 2*DATA_SURFACE_MARGIN + TOP_VIEW_PLATE_WIDTH + SCOREBOARD_WIDTH, height - DATA_SURFACE_HEIGHT);
	}
	
	/**
	 * @param newScore new score value
	 * updates score variable
	 * score has lower and upper bounds
	 */
	public void updateScore(float newScore){
		if(newScore > MAX_SCORE){
			score = MAX_SCORE;
		}
		else if(newScore < MIN_SCORE){
			score = MIN_SCORE;
		}
		else{
			lastScore = newScore - score;
			score = newScore;
		}
	}
	
	/**
	 * returns given number in String format with only two digits after the ","
	 */
	public String roundNumber(float n){
		return String.format("%.02f", n);
	}
	
	public void keyPressed(){
		if(key == CODED){
			if(keyCode == LEFT){
				rotateY -= rotateSpeedY;
			}
			else if(keyCode == RIGHT){
				rotateY += rotateSpeedY;
			}
			if(keyCode == SHIFT){
				shiftView = true;
				//TODO find a way to pause the timer when in shiftMode
				//timer.cancel();
			}
		}
	}
	
	public void keyReleased(){
		shiftView = false;
	}
	
	//clicking on the plate in shift mode draws a cylinder on the clicked position
	public void mousePressed(){
		if(shiftView){
			float x = mouseX - width/2.0f;
			float y = mouseY - height/2.0f;
			boolean onPlate = (x < PLATE_WIDTH/2 && x > -PLATE_WIDTH/2 && y < PLATE_DEPTH/2 && y > -PLATE_DEPTH/2);
			boolean notOnBall = (dist(ball.location.x, ball.location.z, x, y) > SPHERE_RADIUS + CYLINDER_RADIUS);
			if(onPlate && notOnBall){
				bowlingPins.add(new PVector(x, y));
			}
		}
	}
	
	public void updateAnglesFromBoard(){
		float rx = imageProcessing.getRotations().x/boardRotateSpeed;
		float ry = imageProcessing.getRotations().y/boardRotateSpeed;
		//smoothRotateX(rx);
		rotateX = rx;
		rotateZ = ry;
		rotateX = constrain(rotateX, MIN_ANGLE, MAX_ANGLE);
		rotateZ = constrain(rotateZ, MIN_ANGLE, MAX_ANGLE);
	}
	
	/**
	 * makes the plate rotate smoothly in order to avoid brutal movements
	 * @throws InterruptedException 
	 */
	public void smoothRotateX(float newX){
		if(newX < rotateX){
			while(rotateX > newX){
				rotateX += 0.005;
			}
		}
		else{
			while(rotateX < newX){
				rotateX -= 0.005;
			}
		}
	}
	
	public void displayBowlingPin(float x, float y){
		pushMatrix();
		translate(x, -PLATE_HEIGHT/2, y);
		//noLights();
		shape(bowlingPin);
		//lights();
		popMatrix();
	}
	
	/**
	 * removes the targets that've been hit
	 */
	public void removeHitPins(){
		for(int i: pinsToBeRemoved){
			//TODO creer une pin clignotante qui disparait au bout de 2 secondes aux coordonnes de la removedPin
			bowlingPins.remove(i);
			audioContext.loadFile("bowlingStrike.mp3").play();
		}
	}
	
	
	/**
	 * class representing a ball (sphere) and defining the way it moves
	 *
	 */
	public class Mover {
		private final static float GRAVITY_CONSTANT = 2.5f;
		private final static float NORMAL_FORCE = 1.0f;
		private final static float MU = 0.2f;
		private final static float FRICTION_MAGNITUDE = NORMAL_FORCE * MU;
		private PVector location;
		private PVector velocity;
		private PVector gravity;
		private PVector friction;	

		public Mover() {
			location = new PVector(0,-(SPHERE_RADIUS + PLATE_HEIGHT/2), 0);
			velocity = new PVector(0, 0, 0);
			gravity = new PVector(0, 0, 0);
			friction = new PVector(0, 0, 0);
		}

		/**
		 * updates ball position according to friction, gravity and its velocity
		 */
		private void update() {
			friction = velocity.get();
			friction.mult(-1);
			friction.normalize();
			friction.mult(FRICTION_MAGNITUDE);
			gravity.x = sin(rotateZ) * GRAVITY_CONSTANT;
			gravity.z = sin(-rotateX) * GRAVITY_CONSTANT;
			velocity.add(gravity);
			velocity.add(friction);
			location.add(velocity);
		}
		
		/**
		 * Handles ball bouncing when it arrives at an edge of the plate
		 */
		private void checkEdges() {    
		    if(location.x + SPHERE_RADIUS >= PLATE_WIDTH/2){
		    	location.x = PLATE_WIDTH/2 - SPHERE_RADIUS;
		    	velocity.x = - velocity.x;
		    }
		    else if(location.x -SPHERE_RADIUS <= -PLATE_WIDTH/2 ){
		    	location.x = -PLATE_WIDTH/2 + SPHERE_RADIUS;
		    	velocity.x = - velocity.x;
		    }
		    
		    if(location.z + SPHERE_RADIUS >= PLATE_DEPTH/2) {
		    	location.z =  PLATE_DEPTH/2 - SPHERE_RADIUS;
		    	velocity.z = - velocity.z;
		    }    
		    
		    else if(location.z - SPHERE_RADIUS <= -PLATE_DEPTH/2 ){
		    	location.z = - PLATE_DEPTH/2 + SPHERE_RADIUS;
		    	velocity.z = - velocity.z;
		    }
		}
		
		/**
		 * handles ball mouvements when it bounces against a cylinder
		 */
		private void checkCylinderCollision(){
			int index = 0;
			for(PVector cyl: bowlingPins){
				PVector n = PVector.sub(new PVector(location.x, location.z), cyl);
				if(dist(location.x, location.z, cyl.x, cyl.y) < CYLINDER_RADIUS + SPHERE_RADIUS){
					if(ball.velocity.mag() > VELOCITY_TRESHOLD){
						updateScore(score + PIN_HIT_BONUS);
						pinsToBeRemoved.add(index);
					}
					PVector velocity2D = new PVector(velocity.x, velocity.z);
					n.normalize();
					location.x = n.x*(CYLINDER_RADIUS+SPHERE_RADIUS)+cyl.x;
					location.z = n.y*(CYLINDER_RADIUS+SPHERE_RADIUS)+cyl.y;
					PVector newVelocity2D = PVector.sub(velocity2D, PVector.mult(n, 2*PVector.dot(velocity2D, n)));
					velocity = new PVector(newVelocity2D.x, 0, newVelocity2D.y);
				}
				index++;
			}
			if(pinsToBeRemoved.size() > 0){
				removeHitPins();
				pinsToBeRemoved.clear();
			}
		}

		/**
		 * displays the sphere on the screen
		 */
		private void display() {
			translate(location.x, location.y, location.z);
			fill(180, 180, 180);
			sphere(SPHERE_RADIUS);
			noFill();
		}

		/**
		 * displays the ball and updates its position.
		 * Also handles bouncing against edges
		 */
		public void show(){
			this.update();
			this.checkEdges();
			this.checkCylinderCollision();
			this.display();
		}
		
		/**
		 * displays the ball on the rectangle when shift mode is activated
		 */
		public void shiftModeShow(){
			pushMatrix();
			translate(location.x, location.z);
			sphere(SPHERE_RADIUS);
			popMatrix();
		}
	}
	
	class HScrollbar {
		
		  float barWidth;  //Bar's width in pixels
		  float barHeight; //Bar's height in pixels
		  float xPosition;  //Bar's x position in pixels
		  float yPosition;  //Bar's y position in pixels
		  
		  float sliderPosition, newSliderPosition;    //Position of slider
		  float sliderPositionMin, sliderPositionMax; //Max and min values of slider
		  
		  boolean mouseOver;  //Is the mouse over the slider?
		  boolean locked;     //Is the mouse clicking and dragging the slider now?

		  /**
		   * @brief Creates a new horizontal scrollbar
		   * 
		   * @param x The x position of the top left corner of the bar in pixels
		   * @param y The y position of the top left corner of the bar in pixels
		   * @param w The width of the bar in pixels
		   * @param h The height of the bar in pixels
		   */
		  HScrollbar (float x, float y, float w, float h) {
		    barWidth = w;
		    barHeight = h;
		    xPosition = x;
		    yPosition = y;
		    
		    sliderPosition = xPosition + barWidth/2 - barHeight/2;
		    newSliderPosition = sliderPosition;
		    
		    sliderPositionMin = xPosition;
		    sliderPositionMax = xPosition + barWidth - barHeight;
		  }

		  /**
		   * @brief Updates the state of the scrollbar according to the mouse movement
		   */
		  void update() {
		    if (isMouseOver()) {
		      mouseOver = true;
		    }
		    else {
		      mouseOver = false;
		    }
		    if (mousePressed && mouseOver) {
		      locked = true;
		    }
		    if (!mousePressed) {
		      locked = false;
		    }
		    if (locked) {
		      newSliderPosition = constrain(mouseX - barHeight/2, sliderPositionMin, sliderPositionMax);
		    }
		    if (abs(newSliderPosition - sliderPosition) > 1) {
		      sliderPosition = sliderPosition + (newSliderPosition - sliderPosition);
		    }
		  }

		  /**
		   * @brief Clamps the value into the interval
		   * 
		   * @param val The value to be clamped
		   * @param minVal Smallest value possible
		   * @param maxVal Largest value possible
		   * 
		   * @return val clamped into the interval [minVal, maxVal]
		   */
		  float constrain(float val, float minVal, float maxVal) {
		    return min(max(val, minVal), maxVal);
		  }

		  /**
		   * @brief Gets whether the mouse is hovering the scrollbar
		   *
		   * @return Whether the mouse is hovering the scrollbar
		   */
		  boolean isMouseOver() {
		    if (mouseX > xPosition && mouseX < xPosition+barWidth &&
		      mouseY > yPosition && mouseY < yPosition+barHeight) {
		      return true;
		    }
		    else {
		      return false;
		    }
		  }

		  /**
		   * @brief Draws the scrollbar in its current state
		   */ 
		  void display() {
		    noStroke();
		    fill(204);
		    rect(xPosition, yPosition, barWidth, barHeight);
		    if (mouseOver || locked) {
		      fill(0, 0, 0);
		    }
		    else {
		      fill(102, 102, 102);
		    }
		    rect(sliderPosition, yPosition, barHeight, barHeight);
		    fill(topView.ambientColor);
		  }

		  /**
		   * @brief Gets the slider position
		   * 
		   * @return The slider position in the interval [0,1] corresponding to [leftmost position, rightmost position]
		   */
		  float getPos() {
		    return (sliderPosition - xPosition)/(barWidth - barHeight);
		  }
		}
	
}
