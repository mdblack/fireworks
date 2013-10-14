package com.blackware.fireworks;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.*;

public class FireworksActivity extends Activity 
{

	public static final int MILLISECONDS_PER_REFRESH=10;
	public static final int MILLISECONDS_PER_FRAME=41;
	public static final int POINTS_PER_EXPLOSION=10;
	float canvas_width=0;
	float canvas_height=0;
	
	ArrayList<Rocket> rockets;
	ArrayList<Particle> particles;
	FireView fireview;
	int rocketWavId,fireworkWavId1,fireworkWavId2;
	SoundPool soundplayer;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		soundplayer = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
		rocketWavId=soundplayer.load(this, R.raw.r, 1);
		fireworkWavId1=soundplayer.load(this, R.raw.f1, 1);
		fireworkWavId2=soundplayer.load(this, R.raw.f2, 1);
		
		particles=new ArrayList<Particle>();
		rockets=new ArrayList<Rocket>();
		
		fireview=new FireView(this);
		setContentView(fireview);
		fireview.invalidate();
		fireview.setOnTouchListener(fireview);
		
		new Thread(new Runnable(){
			public void run(){
				while(true)
				{
					fireview.postInvalidate();
					try{Thread.sleep(MILLISECONDS_PER_FRAME);}catch(InterruptedException e){}
				}
			}
		}).start();
	}

	public void fireRocket(float x, float y)
	{
		rockets.add(new Rocket(x,y));
		soundplayer.play(rocketWavId, .99f, .99f, 0, 0, 1);
	}
	public void addParticle(float x, float y)
	{
		particles.add(new Particle(x,y,0,1,Color.RED));
	}
	public void explosion(float x, float y)
	{
		if (particles.size()>1000)
			return;
		
		int points=(int)(POINTS_PER_EXPLOSION*(1+Math.random()));

		int color=Color.argb(255, (int)(Math.random()*255), (int)(Math.random()*255), (int)(Math.random()*255));

		for (float angle=0; angle<2*Math.PI; angle+=2*Math.PI/points)
		{
			particles.add(new Particle(x,y,angle,canvas_width/20,color));
		}
		soundplayer.play(fireworkWavId2, .99f, .99f, 0, 0, 1);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		fireRocket((float)(Math.random()*canvas_width),canvas_height);
		return false;
	}
	
	public class FireView extends View implements View.OnTouchListener
	{

		public FireView(Context context) {
			super(context);
		}
		protected void onDraw(Canvas c)
		{
			canvas_width=c.getWidth();
			canvas_height=c.getHeight();
			c.drawColor(Color.BLACK);
			for (int p=0; p<particles.size(); p++)
			{
				synchronized(particles)
				{
					try
					{
						Particle particle=particles.get(p);
						particle.draw(c);
					}
					catch(NullPointerException e){}
					catch(IndexOutOfBoundsException e){}
				}
			}
			for (int r=0; r<rockets.size(); r++)
			{
				synchronized(rockets)
				{
					try
					{
						Rocket rocket=rockets.get(r);
						rocket.draw(c);
					}
					catch(NullPointerException e){}
					catch(IndexOutOfBoundsException e){}
				}
			}
			
		}
		public boolean onTouch(View v, MotionEvent event) 
		{
			if (event.getAction()==MotionEvent.ACTION_DOWN)
			{
				float x=event.getX(); float y=event.getY();
				if (y>canvas_height*0.8)
					fireRocket(event.getX(),event.getY());
				else
					explosion(event.getX(),event.getY());
//				addParticle(event.getX(),event.getY());
			return true;
			}
			return false;
		}
		
	}
	
	public class Rocket implements Runnable
	{
		float explosionx,explosiony;
		boolean alive=true;
		float vx,vy;
		float x,y;
		float gvy=0;
		int color=Color.WHITE;
		public Rocket(float x, float y)
		{
			explosionx=(float)(Math.random()*canvas_width);
			explosiony=(float)(Math.random()*canvas_height*0.7);
			vx=(explosionx-x)/MILLISECONDS_PER_REFRESH/4;
			vy=(explosiony-y)/MILLISECONDS_PER_REFRESH/4;
			this.x=x; this.y=y;
			try{new Thread(this).start();}
			catch(OutOfMemoryError e){}
		}
		public void run()
		{
			while(alive)
			{
				move();
				try{Thread.sleep(MILLISECONDS_PER_REFRESH);}catch(InterruptedException e){}
			}
			
		}
		private void move()
		{
			float dist=(explosionx-x)*(explosionx-x)+(explosiony-y)*(explosiony-y);
			x+=vx*(1.0/MILLISECONDS_PER_REFRESH);
			y+=vy*(1.0/MILLISECONDS_PER_REFRESH)+gvy*(1.0/MILLISECONDS_PER_REFRESH);
			
			if ((explosionx-x)*(explosionx-x)+(explosiony-y)*(explosiony-y)>=dist)
				{
					synchronized(rockets)
					{
						alive=false;
						rockets.remove(this);
					}
					soundplayer.play(fireworkWavId1, .99f, .99f, 0, 0, 1);
					explosion(x,y);
				}
						
			
//			vx=(float)(vx*0.99);
//			vy=(float)(vy*0.99);
//			gvy+=3.0*(1.0/MILLISECONDS_PER_REFRESH);
						
			if (Math.random()<2*(1.0/MILLISECONDS_PER_REFRESH) && particles.size()<200)
			{
				try{
					synchronized(particles){
						particles.add(new Particle(this));
					}
				}catch(ConcurrentModificationException e){}
			}
			
		}
		public void draw(Canvas c)
		{
			if(!alive) return;
			
			Paint paint=new Paint();
			paint.setColor(color);
			if (Math.random()<0.8)  //twinkle
				c.drawCircle(x, y, canvas_width/200, paint);
		}
	}
	
	public class Particle implements Runnable
	{
		float x, y;
		boolean alive;
		int color;
		float vx,vy,gvy=0;
		float brightness;
		boolean debris;
	
		public Particle(Particle p)
		{
			x=p.x; y=p.y; alive=true; color=p.color; gvy=p.gvy;
			brightness=(float)(p.brightness*.7);
			debris=true;
			try{new Thread(this).start();}
			catch(OutOfMemoryError e){}
		}
		public Particle(Rocket r)
		{
			x=r.x; y=r.y; alive=true; color=r.color; gvy=r.gvy;
			brightness=(float)(.7);
			debris=true;
			try{new Thread(this).start();}
			catch(OutOfMemoryError e){}			
		}
		
		public Particle(float x, float y, float angle, float init_v, int color)
		{
			this.x=x; this.y=y;
			init_v+=(float)(0.3*(Math.random()*init_v-init_v/2));
			vx=(float)(init_v*Math.sin(angle));
			vy=(float)(init_v*Math.cos(angle));
			debris=false;
			alive=true;
			this.color=color;
			brightness=(float)1;
			try
			{
				new Thread(this).start();
			}
			catch(OutOfMemoryError e){}
		}
		
		private void move()
		{
			if (!debris)
			{
				x+=vx*(1.0/MILLISECONDS_PER_REFRESH);
				y+=vy*(1.0/MILLISECONDS_PER_REFRESH)+gvy*(1.0/MILLISECONDS_PER_REFRESH);
			
				vx=(float)(vx*0.99);
				vy=(float)(vy*0.99);
				gvy+=3.0*(1.0/MILLISECONDS_PER_REFRESH);
			}
//			brightness=(float)(brightness*0.99);
			if (!debris)
				brightness=(float)(brightness-0.001/MILLISECONDS_PER_REFRESH);
			else
				brightness=(float)(brightness-0.05/MILLISECONDS_PER_REFRESH);				
			if (brightness<0)brightness=0;
			
			if (!debris && Math.random()<2*(1.0/MILLISECONDS_PER_REFRESH) && particles.size()<200)
			{
				try{
					synchronized(particles){
						particles.add(new Particle(this));
					}
				}catch(ConcurrentModificationException e){}
			}
			
/*			if (!debris && Math.random()<0.001*(1.0/MILLISECONDS_PER_REFRESH) && particles.size()<200)
			{
				explosion(x,y);
			}
*/			
			if (x>canvas_width || x<0 || y>canvas_height || y<0 || brightness==0)
			{
				alive=false;
				synchronized(particles){
				particles.remove(this);
				}
			}
		}
		
		public void run()
		{
			while(alive)
			{
				move();
				try{Thread.sleep(MILLISECONDS_PER_REFRESH);}catch(InterruptedException e){}
			}
		}
		
		public void draw(Canvas c)
		{
			if(!alive) return;
			
			Paint paint=new Paint();
			float[] hsv=new float[3];
			Color.colorToHSV(color, hsv);
			hsv[1]*=brightness;
			int paintcolor=Color.HSVToColor(hsv);
			paintcolor=Color.argb(255, (int)(Color.red(paintcolor)*brightness), (int)(Color.green(paintcolor)*brightness), (int)(Color.blue(paintcolor)*brightness));
			paint.setColor(paintcolor);
//			paint.setColor(Color.argb(255, (int)(Color.red(color)*brightness), (int)(Color.green(color)*brightness), (int)(Color.blue(color)*brightness)));
			if (Math.random()<0.8)  //twinkle
				c.drawCircle(x, y, canvas_width/200, paint);
		}
	}
}
