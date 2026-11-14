#include <GL/glu.h>
#include <math.h>
#include <stdlib.h>

struct GLUquadric {
	GLenum normals;
	GLboolean texture;
};

const GLubyte *gluErrorString(GLenum error)
{
	switch (error)
	{
	case GL_NO_ERROR: return (const GLubyte *)"no error";
	case GL_INVALID_ENUM: return (const GLubyte *)"invalid enum";
	case GL_INVALID_VALUE: return (const GLubyte *)"invalid value";
	case GL_INVALID_OPERATION: return (const GLubyte *)"invalid operation";
	case GL_OUT_OF_MEMORY: return (const GLubyte *)"out of memory";
	default: return (const GLubyte *)"unknown OpenGL error";
	}
}

GLUquadric *gluNewQuadric(void)
{
	GLUquadric *quadric = (GLUquadric *)calloc(1, sizeof(*quadric));
	if (quadric)
	{
		quadric->normals = GLU_SMOOTH;
		quadric->texture = GL_FALSE;
	}
	return quadric;
}

void gluDeleteQuadric(GLUquadric *quadric)
{
	free(quadric);
}

void gluQuadricNormals(GLUquadric *quadric, GLenum normals)
{
	if (quadric) quadric->normals = normals;
}

void gluQuadricTexture(GLUquadric *quadric, GLboolean texture)
{
	if (quadric) quadric->texture = texture;
}

void gluSphere(GLUquadric *quadric, GLdouble radius, GLint slices, GLint stacks)
{
	const double pi = 3.14159265358979323846;
	int stack, slice;

	if (!quadric || slices < 3 || stacks < 2) return;

	for (stack = 0; stack < stacks; stack++)
	{
		double lat0 = -pi * 0.5 + pi * stack / stacks;
		double lat1 = -pi * 0.5 + pi * (stack + 1) / stacks;

		glBegin(GL_TRIANGLE_STRIP);
		for (slice = 0; slice <= slices; slice++)
		{
			double lon = 2.0 * pi * slice / slices;
			double u = (double)slice / slices;
			double v0 = (double)stack / stacks;
			double v1 = (double)(stack + 1) / stacks;
			double x = cos(lon);
			double y = sin(lon);
			double z0 = sin(lat0), r0 = cos(lat0);
			double z1 = sin(lat1), r1 = cos(lat1);

			if (quadric->texture) glTexCoord2d(u, v1);
			if (quadric->normals != GLU_NONE) glNormal3d(x * r1, y * r1, z1);
			glVertex3d(radius * x * r1, radius * y * r1, radius * z1);

			if (quadric->texture) glTexCoord2d(u, v0);
			if (quadric->normals != GLU_NONE) glNormal3d(x * r0, y * r0, z0);
			glVertex3d(radius * x * r0, radius * y * r0, radius * z0);
		}
		glEnd();
	}
}
