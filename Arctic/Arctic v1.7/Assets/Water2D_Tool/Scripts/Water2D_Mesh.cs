using UnityEngine;
using System.Collections;
using System.Collections.Generic;

namespace Water2DTool
{
    public class Water2D_Mesh
    {
        #region Fields and Properties
        private List<Vector3> meshVerts;
        private List<int> meshIndices;
        private List<Vector2> meshUVs;
        #endregion

        #region Constructor
        public Water2D_Mesh()
        {
            meshVerts = new List<Vector3>();
            meshUVs = new List<Vector2>();
            meshIndices = new List<int>();
        }
        #endregion

        #region General Methods
        /// <summary>
        /// Clears all verices, indices, uvs, and colors from this mesh, resets color to white.
        /// </summary>
        public void Clear()
        {
            meshVerts.Clear();
            meshIndices.Clear();
            meshUVs.Clear();
        }

        /// <summary>
        /// Clears out the mesh, fills in the data, and recalculates normals and bounds.
        /// </summary>
        /// <param name="mesh">An already existing mesh to fill out.</param>
        public void Build(ref Mesh mesh)
        {
            // round off a few decimal points to try and get better pixel-perfect results
            for (int i = 0; i < meshVerts.Count; i += 1)
                meshVerts[i] = new Vector3(
                         (float)System.Math.Round(meshVerts[i].x, 3),
                         (float)System.Math.Round(meshVerts[i].y, 3),
                         (float)System.Math.Round(meshVerts[i].z, 3));

            mesh.Clear();
            mesh.vertices = meshVerts.ToArray();
            mesh.uv = meshUVs.ToArray();
            mesh.triangles = meshIndices.ToArray();

            mesh.RecalculateBounds();
            mesh.RecalculateNormals();
        }

        #endregion

        #region Vertex and Face Methods

        /// <summary>
        /// Generates triangles from a list of vertices.
        /// </summary>
        /// <param name="widthSegments">The number of horizontal segments.</param>
        /// <param name="heightSegments">The number of vertical segments.</param>
        /// <param name="hVertices">The number of horizontal vertices.</param>
        public void GenerateTriangles(int widthSegments, int hVertices)
        {
            for (int x = 0; x < widthSegments; x++)
            {
                meshIndices.Add(x);
                meshIndices.Add(hVertices + x);
                meshIndices.Add(x + 1);

                meshIndices.Add(hVertices + x);
                meshIndices.Add(hVertices + x + 1);
                meshIndices.Add(x + 1);
            }
        }

        /// <summary>
        /// Adds a vertex to the meshVerts list and a UV point to the meshUVs list.
        /// </summary>
        /// <param name="vertexPoss">The position of a vertex.</param>
        /// <param name="aZ">The position of a vertex on the Z axis.</param>
        /// <param name="aUV">The UV coordinate of the current vertex.</param>
        public void AddVertex(Vector2 vertexPoss, float aZ, Vector2 aUV)
        {
            meshVerts.Add(new Vector3(vertexPoss.x, vertexPoss.y, aZ));
            meshUVs.Add(aUV);
        }
        #endregion
    }
}
