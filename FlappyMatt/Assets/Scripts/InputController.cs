using UnityEngine;
using System.Collections;

public class InputController : MonoBehaviour {

	public string ButtonName = "Fire1";
	public string TitleScene = "Flappy_Title";

	private Flappy myFlappy;

	// Use this for initialization
	void Start () 
	{
		myFlappy = GetComponent<Flappy> ();
	}
	
	// Update is called once per frame
	void Update () 
	{
		bool isFlapping = Input.GetButtonDown (ButtonName);

		if(isFlapping)
		{
			myFlappy.Flap ();
		}

		if(Input.GetKeyDown("escape"))
		{
			Application.LoadLevel(TitleScene);
		}
	}
}
