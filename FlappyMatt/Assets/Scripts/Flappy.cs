using UnityEngine;
using System.Collections;

public class Flappy : MonoBehaviour {
	public string FlapTrigger = "FlapTrigger";
	public string ObstacleName = "Wall";
	public string TitleScene = "Flappy_Title";

	public AudioClip flapSound;

	public float flapPower;

	Rigidbody2D rb;
	Animator anim;
	AudioSource aud;

	// Use this for initialization
	void Start () {
		rb = GetComponent<Rigidbody2D> ();
		anim = GetComponent<Animator> ();
		aud = GetComponent<AudioSource> ();
	}
	
	// Update is called once per frame
	void OnTriggerEnter2D(Collider2D other)
    {
        if(other.CompareTag(ObstacleName))
        {
            Debug.Log("Died!");
            Application.LoadLevel(TitleScene);
        }	
	}

	public void Flap()
	{
		Vector2 flapForce = new Vector2 (0f, flapPower);
		//rb.AddForce (flapForce);
		rb.velocity = flapForce;
		anim.SetTrigger (FlapTrigger);
		aud.clip = flapSound;
		aud.Play ();
	}
}
