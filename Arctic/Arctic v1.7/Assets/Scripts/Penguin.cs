﻿using UnityEngine;
using System.Collections;

public enum movementType
{
    walking, //the default
    sliding, //if the player presses down
    swimming //when in water
}

public class Penguin : MonoBehaviour {

    static public Penguin S; //Singleton for the Penguin class

	public KeyCode up;
	public KeyCode down;

    //can we get some comments on these variables?
    public float maxWalkSpeed = 1.75f;      //max speed for penguin walk
    public float maxSlideSpeed = 3.5f;      //max speed while sliding
	public bool facingRight = true;			//is the penguin facing the right?
	public float tiltSpeed = 10f;			//for touch func
	public float filter = 5.0f;				//for touch func
	public float accelerationSpeed = 10f;	//acceleration
	public bool jump = false;				//if jump == true then groundcheck is turned off
	public bool grounded = false;			//condition if the penguin on the ground
	public Transform groundCheck;			//used to check whether the penguin is on the ground
	public float groundRadius = 0.2f;		//radius of groundCheck
	public LayerMask whatIsGround;			//finds the LayerMask for groundcheck
	public float jumpVelocity = 3;          //vertical velocity applied when jump == true
    public float slideForce = 2f;           //downward force on penguin, causing it to slide
    public movementType movement = movementType.walking; //movement enum - set to walking by default

    //======= Private Variables =======\\

    private Rigidbody2D rigidbod; //Rigidbody2D of Penguin
    private Vector3 accel;
    private Animator anim;

    void Awake()
    {
        S = this; //assign the singleton
    }
	
	// Use this for initialization
	void Start () {
		anim = GetComponent<Animator>();
        rigidbod = GetComponent<Rigidbody2D>();
		accel = Input.acceleration;
	}
	
	// Update is called once per frame
	void FixedUpdate ()
    {
		
		grounded = Physics2D.OverlapCircle(groundCheck.position, groundRadius, whatIsGround);
		//anim.SetBool("Ground", grounded);		
		//anim.SetFloat ("vSpeed", GetComponent<Rigidbody2D>().velocity.y);
		
	/*	
		if (grounded && ((Input.GetKeyDown(up)) || Input.touchCount == 1)) 
     rigidbody2D.AddForce(new Vector2(0, Mathf.Clamp(jumpVelocity, 0, 700)));
   */
		if(jump)
        {
			//rigidbody.AddForce(new Vector2(0, jumpVelocity));
            //rigidbody2D.AddForce (new Vector2(0, jumpVelocity) * rigidbody2D.mass / Time.fixedDeltaTime);

            rigidbod.velocity = new Vector2(0, jumpVelocity);//this is the most natural looking I think
		}
		
		// Set animation
		if ((Application.platform == RuntimePlatform.Android) || (Application.platform == RuntimePlatform.IPhonePlayer))
        {
			// filter the jerky acceleration in the variable accel:
			accel = Vector3.Lerp(accel, Input.acceleration, filter * Time.deltaTime);

			// map accel -Y and X to game X and Y directions:
			Vector3 dir = new Vector3(Mathf.Clamp(accel.x, -accelerationSpeed, accelerationSpeed), 0, 0);

			// limit dir vector to magnitude 1:
			if (dir.sqrMagnitude > 1) dir.Normalize();

			// move the object at the velocity defined in speed:
			transform.Translate(dir * tiltSpeed * Time.deltaTime);
			anim.SetFloat("Speed", Mathf.Abs(dir.sqrMagnitude * tiltSpeed));

            //flip the object accordingly
			if (Input.acceleration.x > 0 && !facingRight)
				Flip ();
			else if (Input.acceleration.x < 0 && facingRight)
				Flip ();
		}
		else
        {
            float move = Input.GetAxis("Horizontal");

            if (movement == movementType.walking)
            {                               
                //anim.SetFloat("Speed", Mathf.Abs(move));
                rigidbod.velocity = new Vector2(move * maxWalkSpeed, rigidbod.velocity.y);

                //rotate penguin -- will need animation to rotate head here                
                transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, 0), Time.deltaTime * 500f);
            }
            else if(movement == movementType.sliding)
            {
                rigidbod.velocity = new Vector2(move * maxSlideSpeed, rigidbod.velocity.y);

                //add force in -y direction for sliding effect
                rigidbod.AddForce(new Vector2(0, slideForce));

                //add greater force to jump maybe

                //rotate penguin -- will need animation to rotate head here     
                if(facingRight)
                {
                    transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, -90), Time.deltaTime * 500f);//rotate face toward ground
                }           
                else if(!facingRight)
                {
                    transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, 90), Time.deltaTime * 500f);//must rotate the other way, otherwise the penguin lies on his back
                }           
                

            }


            // Decide what way animation moving
            if (move > 0 && !facingRight)
				Flip ();
			else if (move < 0 && facingRight)
				Flip ();
		}

		jump = false;

	}//end FixedUpdate
	
	void Flip () {
		facingRight = !facingRight;
		Vector3 thescale = transform.localScale;

        if(movement == movementType.walking)
        {
            thescale.x *= -1;//if walking, flip along x           
        }
		else if(movement == movementType.sliding)
        {
            //thescale.x *= -1;
            thescale.y *= -1;//if sliding, flip along x and y
        }
        transform.localScale = thescale;
    }
	
	void Update () {
		if(grounded && ((Input.GetKeyDown(up)) || Input.touchCount == 1)){
			anim.SetBool("Ground", false);
			//rigidbody2D.AddForce(new Vector2(0, jumpVelocity * Time.fixedDeltaTime));
			jump = true;
		}

        if(Input.GetAxisRaw("Vertical") == -1)//not sure how to handle touch input here
        {
            //Debug.Log("Vertical Axis down");
            movement = movementType.sliding; //we are sliding
        }
        else
        {
            movement = movementType.walking; //we are walking
        }

	}
	void OnTriggerEnter2D(Collider2D c) {//stops penguin from looping infinitely
		if (c.CompareTag ("LetGo")) {//after loop
			this.transform.parent=null;//penguin is disowned of being the loop's child
		}
	}
}