#version 420 core

#define POSITION	0
#define COLOR		3
#define TEXCOORD	4
#define FRAG_COLOR	0

#define MATERIAL	0
#define TRANSFORM0	1
#define TRANSFORM1	2

precision highp float;
precision highp int;
layout(std140, column_major) uniform;

layout(location = FRAG_COLOR, index = 0) out vec4 color;

void main()
{
    color = vec4(1.0, 0.5, 0.0, 1.0);
}