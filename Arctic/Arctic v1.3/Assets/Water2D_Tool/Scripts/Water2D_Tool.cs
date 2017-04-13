using UnityEngine;
using System.Collections;
using System.Collections.Generic;

namespace Water2DTool
{
    [RequireComponent(typeof(MeshFilter)), RequireComponent(typeof(MeshRenderer))]
    public class Water2D_Tool : MonoBehaviour
    {
        #region Private fields
        /// The water mesh. 
        /// </summary>
        private Mesh mesh;
        /// <summary>
        /// Water2D_Mesh instance. 
        /// </summary>
        private Water2D_Mesh dMesh;
        /// <summary>
        /// Water2D_Mesh property. 
        /// </summary>
        private Water2D_Mesh DMesh
        {
            get
            {
                if (dMesh == null)
                    dMesh = new Water2D_Mesh();
                return dMesh;
            }
        }
        /// <summary>
        /// The number of horizontal vertices. 
        /// </summary>
        private int hVerts = 1;
        /// <summary>
        /// The number of vertical vertices. 
        /// </summary>
        private int vVerts = 1;
        /// <summary>
        /// The width of a horizontal segment. 
        /// </summary>
        private float scaleX = 1f;
        /// <summary>
        /// The number of vertical segments. This value must not be changed.
        /// </summary>
        private int heightSegments = 1;
        #endregion

        #region Public fields
        /// <summary>
        /// The max width and height the water can have without tiling. 
        /// </summary>
        public Vector2 unitsPerUV = Vector2.one;
        /// <summary>
        /// The number of horizontal segments. 
        /// </summary>
        public int widthSegments = 1;
        /// <summary>
        /// The water width. 
        /// </summary>
        public float width = 1.0f;
        /// <summary>
        /// The water height. 
        /// </summary>
        public float height = 1.0f;
        /// <summary>
        /// How many pixels should be in one unit of Unity space.
        /// </summary>
        public float pixelsPerUnit = 100;
        /// <summary>
        /// The number of horizontal segments that should fit into one unit of Unity space.
        /// </summary>
        public float segmentsPerUnit = 3f;
        /// <summary>
        /// Should the material be tiled verticaly.
        /// </summary>
        public bool verticalTiling;
        /// <summary>
        /// Shows the water mesh shape in the Scene View.
        /// </summary>
        public bool showMesh;
        /// <summary>
        /// Should we generate a collider on Start.
        /// </summary>
        public bool createCollider = true;
        /// <summary>
        /// Colliders top edge offset.
        /// </summary>
        public float colliderOffset = 0.5f;
        /// <summary>
        /// A list with the local positions of the 4 handles. Do not change the handles order. 
        /// A lot of Water2D code will not correctly if you change the order of the handles in the list. 
        /// 0 - top handle.
        /// 1 - bottom handle.
        /// 2 - left handle.
        /// 3 - right handle.
        /// </summary>
        public List<Vector2> handlesPosition = new List<Vector2>();
        /// <summary>
        /// The value of this field is not used in any important calculations. When creating an animation that animates
        /// the area of the water, use this value as a guide to see how the water area changes between two positions.
        /// </summary>
        public float curentWaterArea = 0f;
        /// <summary>
        /// It's used to decide what collider the water should have. A 2D or a 3D collider.
        /// </summary>
        public bool use3DCollider = false;
        /// <summary>
        /// The offset of the water's Box Collider on the Z axis.
        /// </summary>
        public float boxColliderZOffset = 0.5f;
        /// <summary>
        /// The size of the water's Box Collider on the Z axis.
        /// </summary>
        public float boxColliderZSize = 1f;
        #endregion

        /// <summary>
        /// Recreates the water mesh and updates the collider.
        /// </summary>
        public void RecreateWaterMesh()
        {
            DMesh.Clear();

            WaterMesh();

            if (!use3DCollider)
                UpdateCollider2D();
            else
                UpdateCollider3D();

            // If the Application is not in the play mode get the MeshFilter component.
            if (!Application.isPlaying)
            {
                mesh = GetComponent<MeshFilter>().sharedMesh;
            }

            // If the Application is not in the play mode and the mesh is null create a new mesh.
            if (!Application.isPlaying && mesh == null)
            {
                // The object doesn't have a mesh so a new mesh is created.
                mesh = GetComponent<MeshFilter>().sharedMesh = GetMesh();
            }

            if (Application.isPlaying && mesh == null)
            {
                // This line will be executed only once.
                mesh = GetComponent<MeshFilter>().sharedMesh;
            }

            DMesh.Build(ref mesh);
        }

        /// <summary>
        /// Adds a BoxCollider2D component if the water doesn't have one and updates its size.
        /// </summary>
        public void UpdateCollider2D()
        {
            if (!createCollider)
            {
                if (GetComponent<BoxCollider2D>() != null)
                {
                    BoxCollider2D collider = GetComponent<BoxCollider2D>();
                    collider.enabled = false;
                }

                return;
            }

            if (GetComponent<BoxCollider2D>() == null)
            {
                gameObject.AddComponent<BoxCollider2D>();
                BoxCollider2D collider = GetComponent<BoxCollider2D>();
                collider.isTrigger = true;
            }

            BoxCollider2D boxCollider2D = GetComponent<BoxCollider2D>();

            if (boxCollider2D.enabled == false)
                boxCollider2D.enabled = true;

            boxCollider2D.size = new Vector2(width, height + colliderOffset);

            Vector2 center = handlesPosition[1];
            center.y += height / 2f + colliderOffset / 2f;

#if (UNITY_4_6 || UNITY_4_5)
            boxCollider2D.center = center;
#else
            boxCollider2D.offset = center;
#endif
        }

        /// <summary>
        /// Adds a BoxCollider component if the water doesn't have one and updates its size.
        /// </summary>
        public void UpdateCollider3D()
        {
            if (!createCollider)
            {
                if (GetComponent<BoxCollider>() != null)
                {
                    BoxCollider collider = GetComponent<BoxCollider>();
                    collider.enabled = false;
                }

                return;
            }

            if (GetComponent<BoxCollider>() == null)
            {
                gameObject.AddComponent<BoxCollider>();
                BoxCollider collider = GetComponent<BoxCollider>();
                collider.isTrigger = true;
            }

            BoxCollider boxCollider = GetComponent<BoxCollider>();

            if (boxCollider.enabled == false)
                boxCollider.enabled = true;

            boxCollider.size = new Vector3(width, height + colliderOffset, boxColliderZSize);

            Vector3 center = handlesPosition[1];
            center.y += height / 2f + colliderOffset / 2f;
            center.z += boxColliderZOffset;
            boxCollider.center = center;
        }

        /// <summary>
        /// Returns the current object mesh or adds a mesh if the object doesn't have one.
        /// </summary>
        private Mesh GetMesh()
        {
            MeshFilter filter = GetComponent<MeshFilter>();
            string newName = GetMeshName();
            Mesh result = filter.sharedMesh;

            if (filter.sharedMesh == null)
            {
                result = new Mesh();
                result.name = newName;
            }

            return result;
        }

        /// <summary>
        /// Creates a name for the mesh.
        /// </summary>
        public string GetMeshName()
        {
            return gameObject.name + gameObject.GetInstanceID() + "-Mesh";
        }

        /// <summary>
        /// Sets the vertices position and UVs.
        /// </summary>
        private void WaterMesh()
        {
            if (GetComponent<Renderer>().sharedMaterial == null)
                return;

            unitsPerUV.x = GetComponent<Renderer>().sharedMaterial.mainTexture.width / pixelsPerUnit;
            unitsPerUV.y = GetComponent<Renderer>().sharedMaterial.mainTexture.height / pixelsPerUnit;

            width = Mathf.Abs(handlesPosition[3].x - handlesPosition[2].x);
            height = Mathf.Abs(handlesPosition[0].y - handlesPosition[1].y);

            widthSegments = (int)(Mathf.Ceil(width / (1f / segmentsPerUnit)));

            hVerts = widthSegments + 1;
            vVerts = heightSegments + 1;

            scaleX = 1f / segmentsPerUnit;

            for (int y = 0; y < vVerts; y++)
            {
                for (int x = 0; x < hVerts; x++)
                {
                    if (y == 0)
                    {
                        Vector3 vertPos = new Vector3(x < hVerts - 1 ? x * scaleX + handlesPosition[2].x : handlesPosition[3].x, handlesPosition[1].y, 0.0f);
                        float U = (vertPos.x + transform.position.x) / unitsPerUV.x;
                        float V = height / unitsPerUV.y;
                        DMesh.AddVertex(vertPos, 0.0f, new Vector2(U, verticalTiling && V > 1 ? 0 : 1 - V));
                    }
                    else
                    {
                        Vector3 vertPos = new Vector3(x < hVerts - 1 ? x * scaleX + handlesPosition[2].x : handlesPosition[3].x, handlesPosition[0].y, 0.0f);
                        float U = (vertPos.x + transform.position.x) / unitsPerUV.x;
                        DMesh.AddVertex(vertPos, 0.0f, new Vector2(U, 1));
                    }
                }
            }

            DMesh.GenerateTriangles(widthSegments, hVerts);
        }


        /// <summary>
        /// Moves the object location to the center of the handles. Also offsets the handles locations to match.
        /// </summary>
        public void ReCenterPivotPoint()
        {
            Vector2 center = Vector2.zero;

            for (int i = 0; i < handlesPosition.Count; i++)
            {
                center += handlesPosition[i];
            }

            center = center / handlesPosition.Count + new Vector2(transform.position.x, transform.position.y);
            Vector2 offset = center - new Vector2(gameObject.transform.position.x, gameObject.transform.position.y);

            for (int i = 0; i < handlesPosition.Count; i++)
            {
                handlesPosition[i] -= offset;
            }

            gameObject.transform.position = new Vector3(center.x, center.y, gameObject.transform.position.z);

            RecreateWaterMesh();
        }

        /// <summary>
        /// Sets the position of a handle when the water is created.
        /// </summary>
        /// <param name="pos">Handle position</param>
        public void Add(Vector2 pos)
        {
            handlesPosition.Add(pos);
        }

        /// <summary>
        /// Sets the initial water material.
        /// </summary>
        public void SetDefaultMaterial()
        {
            Renderer rend = GetComponent<Renderer>();
            Material m = Resources.Load("Default_Material", typeof(Material)) as Material;
            if (m != null)
            {
                rend.material = m;

                unitsPerUV.x = GetComponent<Renderer>().sharedMaterial.mainTexture.width / pixelsPerUnit;
                unitsPerUV.y = GetComponent<Renderer>().sharedMaterial.mainTexture.height / pixelsPerUnit;
            }
            else
            {
                Debug.LogWarning("The default material was not found. This happened most likely because you moved Water2D_Tool from the "
                    + " Assets folder to a subfolder, deleted or renamed the Default_Material from the Resources folder. Click on this "
                    + "message to set the name of the default material if you renamed it.");
            }
        }
    }
}
