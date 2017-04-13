using UnityEngine;
using System.Collections;

public class Parallax : MonoBehaviour {

    public GameObject poi;//The player ship
    public GameObject[] panels;//The scrolling foreground
    public float scrollSpeed = -30f;

    //motionMult controls how much panels react to player movement
    public float motionMult = 0.25f;

    private float panelHt; //height of each panel
    private float depth;//depth of panels (that is, pos.z)

    void Start()
    {
        panelHt = panels[0].transform.localScale.y;
        depth = panels[0].transform.position.z;

        //set initial positions of panels
        panels[0].transform.position = new Vector3(0, 0, depth);
        panels[1].transform.position = new Vector3(0, panelHt, depth);
    }

    void Update()
    {
        float tY, tX = 0;
        tY = Time.time * scrollSpeed % panelHt + (panelHt * 0.5f);

        if(poi != null)
        {
            tX = -poi.transform.position.x * motionMult;
        }

        //Position panels[0]
        panels[0].transform.position = new Vector3(tX, tY, depth);

        //then position panels[1] where needed to make a continuous starfield
        if(tY >= 0)
        {
            panels[1].transform.position = new Vector3(tX, tY - panelHt, depth);
        }
        else
        {
            panels[1].transform.position = new Vector3(tX, tY + panelHt, depth);
        }
    }
}
