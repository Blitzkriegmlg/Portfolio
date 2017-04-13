using UnityEngine;
using System.Collections;

namespace Water2DTool
{
    // This script will display 2 buttons on the Game Screen.
    // Must be attached to the Main Camera.
    // You must add the scenes to the Build and Run first, otherwise you will
    // get an error when pressing the buttons.
    public class SceneMenu : MonoBehaviour
    {

        void OnGUI()
        {
            if (GUI.Button(new Rect(Screen.width / 2f - 105f, 25, 100f, 30f), "Scene 1"))
            {
                Application.LoadLevel("SandBox_01");
            }

            if (GUI.Button(new Rect(Screen.width / 2f + 5, 25, 100f, 30f), "Scene 2"))
            {
                Application.LoadLevel("SandBox_02");
            }
        }
    }
}
