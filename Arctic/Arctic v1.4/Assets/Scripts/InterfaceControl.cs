using UnityEngine;
using System.Collections;

public class InterfaceControl : MonoBehaviour {

	void RespawnPlayer(){//respawns player to starting position if needed
		ResetScore ();//reset score on respawn
		Application.LoadLevel(Application.loadedLevel);//load current level on
	}
	void OnGUI(){
		GUI.Window (0,new Rect (900, 0, 120, 80),myWindow,"Your Score:"+FishCoin.score);
	}
	void myWindow(int winID){
		if (GUILayout.Button ("Start Menu"))
			Application.LoadLevel (0);
		if (GUILayout.Button ("Exit"))
			Application.Quit ();
	}
	void ResetScore(){
		FishCoin.score = 0;
	}
}
