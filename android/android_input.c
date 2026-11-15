#include <jni.h>
#include <stdio.h>
#include <unistd.h>
#include <SDL.h>

static void pushKeyEvent(Uint32 type, SDL_Keycode keycode)
{
	SDL_Event event;
	SDL_zero(event);
	event.type = type;
	event.key.type = type;
	event.key.state = (type == SDL_KEYDOWN) ? SDL_PRESSED : SDL_RELEASED;
	event.key.keysym.sym = keycode;
	event.key.keysym.scancode = SDL_GetScancodeFromKey(keycode);
	event.key.keysym.mod = SDL_GetModState();
	event.key.repeat = 0;

	if (SDL_PushEvent(&event) < 0)
	{
		fprintf(stderr, "Could not queue Android virtual key: %s\n", SDL_GetError());
	}
}

JNIEXPORT void JNICALL
Java_org_te4_android_TE4Activity_nativeSendKey(JNIEnv *env, jclass type, jint keycode)
{
	(void)env;
	(void)type;
	if (!SDL_WasInit(SDL_INIT_EVENTS)) return;

	pushKeyEvent(SDL_KEYDOWN, (SDL_Keycode)keycode);
	pushKeyEvent(SDL_KEYUP, (SDL_Keycode)keycode);
}

static void pushMouseButtonEvent(Uint32 type, Uint8 button, int x, int y)
{
	SDL_Event event;
	SDL_zero(event);
	event.type = type;
	event.button.type = type;
	event.button.button = button;
	event.button.state = (type == SDL_MOUSEBUTTONDOWN) ? SDL_PRESSED : SDL_RELEASED;
	event.button.clicks = 1;
	event.button.x = x;
	event.button.y = y;

	if (SDL_PushEvent(&event) < 0)
	{
		fprintf(stderr, "Could not queue Android virtual mouse button: %s\n", SDL_GetError());
	}
}

JNIEXPORT void JNICALL
Java_org_te4_android_TE4Activity_nativeSendMouseButton(JNIEnv *env, jclass type, jint button)
{
	int x = 0, y = 0;
	(void)env;
	(void)type;
	if (!SDL_WasInit(SDL_INIT_VIDEO)) return;
	if (button != SDL_BUTTON_LEFT && button != SDL_BUTTON_RIGHT) return;

	SDL_GetMouseState(&x, &y);
	pushMouseButtonEvent(SDL_MOUSEBUTTONDOWN, (Uint8)button, x, y);
	pushMouseButtonEvent(SDL_MOUSEBUTTONUP, (Uint8)button, x, y);
}

JNIEXPORT void JNICALL
Java_org_te4_android_TE4Activity_nativeSetTextInput(JNIEnv *env, jclass type, jboolean enabled)
{
	(void)env;
	(void)type;
	if (!SDL_WasInit(SDL_INIT_VIDEO)) return;

	if (enabled) SDL_StartTextInput();
	else SDL_StopTextInput();
}

JNIEXPORT jboolean JNICALL
Java_org_te4_android_TE4Activity_nativeRedirectLog(JNIEnv *env, jclass type, jint fd)
{
	(void)env;
	(void)type;
	if (fd < 0) return JNI_FALSE;

	fflush(stdout);
	fflush(stderr);
	if (dup2(fd, STDOUT_FILENO) < 0 || dup2(fd, STDERR_FILENO) < 0)
		return JNI_FALSE;
	if (setvbuf(stdout, NULL, _IONBF, 0) != 0
		|| setvbuf(stderr, NULL, _IONBF, 0) != 0)
		return JNI_FALSE;

	return JNI_TRUE;
}
