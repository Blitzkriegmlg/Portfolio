using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class Menu : MonoBehaviour {

	public Canvas quitMenu;
	public Button startText;
	public Button exitText;

	void Start () {
		quitMenu = quitMenu.GetComponent<Canvas> ();//find quit texts parent canvas
		startText = startText.GetComponent<Button> ();//grab the button components of start
		exitText = exitText.GetComponent<Button> ();//grab the button components of exit
		quitMenu.enabled = false; // disable quit menu on start up
	
	}
	public void ExitPress () {
		quitMenu.enabled = true;
		startText.enabled = false; 
		exitText.enabled = false;
	}

	public void NoPress(){
		quitMenu.enabled = false;
		startText.enabled = true;
		exitText.enabled = true;
	}
	public void StartLevel(){
		//looks for first level set in build settings
		Application.LoadLevel (1);
	}
	public void ExitGame(){
		Application.Quit ();
	}

}
