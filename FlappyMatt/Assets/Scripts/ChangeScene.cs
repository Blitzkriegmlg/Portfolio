using UnityEngine;
using System.Collections;

public class ChangeScene : MonoBehaviour {

	public string ButtonName = "Fire1";
	public string NextScene = "Play_Scene";

	// Use this for initialization
	void Start () {

	
	}
	
	// Update is called once per frame
	void Update () {
		bool isPressed = Input.GetButtonDown (ButtonName);

		//load the next level
		if (isPressed)
		{
			Application.LoadLevel(NextScene);
		}

		if(Input.GetKeyDown (KeyCode.Escape))
		{
			Application.Quit();
		}
	}
}
