using UnityEngine;
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

    public float walkForce = 10f;           //force from input when walking
    public float maxWalkSpeed = 4f;         //max speed for penguin walk
    public float slideForce = 2f;           //force from input when sliding
    public float maxSlideSpeed = 3.5f;      //max speed while sliding
    public float swimForce = 4f;            //force from input when swimming
    public float maxSwimSpeed = 6f;         //max speed while swimming
    public float sinkSpeed = 4f;            //contant speed applied downward when in water
    public float exitWaterLeapForce = 150f; //upward force when jumping out of water
	public bool facingRight = true;			//is the penguin facing the right?
	public float tiltSpeed = 10f;			//for touch func
	public float filter = 5.0f;				//for touch func
	public float accelerationSpeed = 10f;	//acceleration
	public bool jump = false;				//if jump == true then groundcheck is turned off
	public bool grounded = false;			//condition if the penguin on the ground
	public Transform groundCheck;			//used to check whether the penguin is on the ground
    public PhysicsMaterial2D pMat;           //adjust friction when walking vs sliding
    public float walkFriction = .5f;        //friction when walking
    public float slideFriction = .1f;       //friction when sliding
    public float groundRadius = 0.2f;		//radius of groundCheck
	public LayerMask whatIsGround;			//finds the LayerMask for groundcheck
	public float jumpVelocity = 3;          //vertical velocity applied when jump == true
    public float rotationSpeed = 800f;      //speed when rotating from on position to another
    public movementType movement = movementType.walking; //movement enum - set to walking by default


    //======= Private Variables =======\\

    private float move;
    private Rigidbody2D rigidbod; //Rigidbody2D of Penguin
    private Vector3 accel;
    private Animator anim;

    //=========== Methods =============\\

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
		
		if(jump)
        {
            rigidbod.velocity = new Vector2(rigidbod.velocity.x, jumpVelocity);//this is the most natural looking I think
		}
		
		// Set animation
		if ((Application.platform == RuntimePlatform.Android) || (Application.platform == RuntimePlatform.IPhonePlayer))//does this get ran?
        {
            MobilePhoneMovement();
		}

		else//everything in this else should be it's own method
        {
            Movement();
		}

		jump = false;

	}//end FixedUpdate

    void Update()
    {
        if ((grounded && ((Input.GetKeyDown(up)) || Input.touchCount == 1)) || //if we're grounded and the player hits jump or we are swimming and the player hits jump
            movement == movementType.swimming && ((Input.GetKeyDown(up)) || Input.touchCount == 1)) 
        {
            //anim.SetBool("Ground", false);//this throws warning that it does not exist

            jump = true;
        }
        if(movement == movementType.swimming)
        {
            return;
        }
        else if (Input.GetAxisRaw("Vertical") == -1)//not sure how to handle touch input here
        {

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

        else if (c.CompareTag("End"))
        {
            //hop to the end and load the menu(for now)
            StartCoroutine(HopToEnd(0));
        }
    }

    void OnTriggerStay2D(Collider2D c)
    {
    if(c.CompareTag("Water")) //if they penguin is within the collider for water
        {
            movement = movementType.swimming; //start swimming       
        }    
    }

    void OnTriggerExit2D(Collider2D c)
    {
        if(c.CompareTag("Water")) //if we jump out of water
        {
            movement = movementType.walking; //turn upright and go back to walking
            rigidbod.AddForce(Vector2.up * exitWaterLeapForce);
        }
    }


    void MobilePhoneMovement()
    {
        Debug.Log("Running as android or Iphone");//this is not being run currently


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
            Flip();
        else if (Input.acceleration.x < 0 && facingRight)
            Flip();
    }

    void Movement()
    {
        //move = Input.GetAxis("Horizontal");
        move = Input.GetAxisRaw("Horizontal"); //GetAxisRaw gives a little bit sharper inputs

        if (movement == movementType.walking) //if we are walking
        {
            pMat.friction = walkFriction;
            //anim.SetFloat("Speed", Mathf.Abs(move));

            if (rigidbod.velocity.magnitude > maxWalkSpeed)
            {
                rigidbod.velocity = Vector2.Lerp(rigidbod.velocity, rigidbod.velocity.normalized * maxWalkSpeed, .05f);//the third parameter of .Lerp() is bound between 0 and 1, check the Unity API
            }
            else
            {
                rigidbod.AddForce(new Vector2(move * walkForce, rigidbod.velocity.y));
            }
        }

        else if (movement == movementType.sliding) //if we are sliding
        {
            pMat.friction = slideFriction;

            if (rigidbod.velocity.magnitude > maxSlideSpeed)
            {
                rigidbod.velocity = Vector2.Lerp(rigidbod.velocity, rigidbod.velocity.normalized * maxSlideSpeed, .05f);//the third parameter of .Lerp() is bound between 0 and 1, check the Unity API
            }
            else
            {
                rigidbod.AddForce(new Vector2(move * slideForce, rigidbod.velocity.y));
            }

        }

        else if(movement == movementType.swimming) //if we are swimming
        {
            //Debug.Log("Checking move - " + move);  
            if(rigidbod.velocity.magnitude > maxSwimSpeed)
            {
                rigidbod.velocity = Vector2.Lerp(rigidbod.velocity, rigidbod.velocity.normalized * maxSwimSpeed, .05f); //Slowly bind the value to the max allowed
            }         
            else
            {
                rigidbod.AddForce(new Vector2(move * swimForce, rigidbod.velocity.y - sinkSpeed));//constant downward velocity
            }
        }

        Flip();

    }//end Movement

    void Flip()
    {
        //code that may need called every FixedUpdate
        if(movement == movementType.sliding || movement == movementType.swimming) //if we should be horizontal
        {
            if (facingRight)
            {
                transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, -90), Time.deltaTime * rotationSpeed);//rotate face toward ground
            }
            else if (!facingRight)
            {
                transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, 90), Time.deltaTime * rotationSpeed);//must rotate the other way, otherwise the penguin lies on his back
            }
        }
        else //we must be walking
        {
            transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, 0), Time.deltaTime * rotationSpeed);//rotate back to normal
        }

        if(move > 0 && !facingRight || move < 0 && facingRight) //do we need to flip?
        {
            facingRight = !facingRight;
            Vector3 thescale = transform.localScale;

            thescale.x *= -1;//flip the sprite

            transform.localScale = thescale;
        }
        else //we don't need to flip
        {
            return;
        }
    }

    IEnumerator HopToEnd(float t)
    {
        //function modeling position after cycloid function
        float startingX = transform.position.x;
        float startingY = transform.position.y;

        //for more on cycloids - http://mathworld.wolfram.com/Cycloid.html
        while(t < Mathf.PI)
        {
            transform.position = new Vector2((t - Mathf.Sin(t) * 0.01f) + startingX,
                                             (1 - Mathf.Cos(t) * 0.01f) + startingY);//give it a new position
            t += 2f;
            yield return new WaitForSeconds(1f);
        }
        Application.LoadLevel("StartMenu");
    }
}