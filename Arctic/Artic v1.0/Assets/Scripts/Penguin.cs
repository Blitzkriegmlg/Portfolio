using UnityEngine;
using System.Collections;

public class Penguin : MonoBehaviour {
	public float maxSpeed = 1f;
	bool faceRight = true;
	Animator anim;
	
	bool grounded=false;
	public Transform groundCheck;
	float groundRadius = 0.2f;
	public LayerMask whatIsGround;
	
	void Start () 
	{
		anim = GetComponent<Animator> ();
	}
	void FixedUpdate () 
	{
		//if result is false, not on ground
		grounded = Physics2D.OverlapCircle (groundCheck.position, groundRadius, whatIsGround);
		anim.SetBool ("Ground", grounded);
		//this lets me know if I'm on the ground or not
		
		float move = Input.GetAxis ("Horizontal");
		anim.SetFloat ("Speed", Mathf.Abs (move));
		GetComponent<Rigidbody2D>().velocity = new Vector2 (move * maxSpeed, GetComponent<Rigidbody2D>().velocity.y);
		if (move > 0 && !faceRight)
			Flip ();
		else if (move < 0 && faceRight)
			Flip ();
	}
	void Flip()
	{
		faceRight = !faceRight;
		Vector3 theScale = transform.localScale;
		theScale.x *= -1;
		transform.localScale = theScale;
	}
}
