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
    public movementType movement = movementType.walking; //movement enum - set to walking by default


    //======= Private Variables =======\\

    private float move;
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
        if (grounded && ((Input.GetKeyDown(up)) || Input.touchCount == 1))
        {
            //anim.SetBool("Ground", false);//this throws warning that it does not exist

            jump = true;
        }

        if (Input.GetAxisRaw("Vertical") == -1)//not sure how to handle touch input here
        {

            movement = movementType.sliding; //we are sliding
        }
        else
        {
            movement = movementType.walking; //we are walking
        }

    }

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
            thescale.x *= -1;//if sliding, flip along x and y
        }
        transform.localScale = thescale;
    }	
	
	void OnTriggerEnter2D(Collider2D c) {//stops penguin from looping infinitely
		if (c.CompareTag ("LetGo")) {//after loop
			this.transform.parent=null;//penguin is disowned of being the loop's child
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
        move = Input.GetAxis("Horizontal");

        if (movement == movementType.walking)
        {
            pMat.friction = walkFriction;
            //anim.SetFloat("Speed", Mathf.Abs(move));

            if (rigidbod.velocity.magnitude > maxWalkSpeed)
            {
                rigidbod.velocity = rigidbod.velocity.normalized * maxWalkSpeed;
            }
            else
            {
                rigidbod.AddForce(new Vector2(move * walkForce, rigidbod.velocity.y));
            }



            //rotate penguin -- will need animation to rotate head here                
            transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, 0), Time.deltaTime * 500f);
        }
        else if (movement == movementType.sliding)
        {
            pMat.friction = slideFriction;

            if (rigidbod.velocity.magnitude > maxSlideSpeed)
            {
                rigidbod.velocity = rigidbod.velocity.normalized * maxSlideSpeed;
            }
            else
            {
                rigidbod.AddForce(new Vector2(move * slideForce, rigidbod.velocity.y));
            }






            //rotate penguin -- will need animation to rotate head here -- should all be handled in Flip()
            if (facingRight)
            {
                transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, -90), Time.deltaTime * 500f);//rotate face toward ground
            }
            else if (!facingRight)
            {
                transform.rotation = Quaternion.RotateTowards(transform.rotation, Quaternion.Euler(0, 0, 90), Time.deltaTime * 500f);//must rotate the other way, otherwise the penguin lies on his back
            }


        }


        // Decide what way animation moving -- call this in Flip()
        if (move > 0 && !facingRight)
            Flip();
        else if (move < 0 && facingRight)
            Flip();
    }
}