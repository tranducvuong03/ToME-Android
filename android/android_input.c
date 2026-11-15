#include <jni.h>
#include <stdio.h>
#include <unistd.h>
#include <SDL.h>
#include <SDL_system.h>

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

JNIEXPORT void JNICALL
Java_org_te4_android_TE4Activity_nativeSetTextInput(JNIEnv *env, jclass type, jboolean enabled)
{
	(void)env;
	(void)type;
	if (!SDL_WasInit(SDL_INIT_VIDEO)) return;

	if (enabled) SDL_StartTextInput();
	else SDL_StopTextInput();
}

void TE4AndroidSetDlcButtonVisible(int visible)
{
	JNIEnv *env = (JNIEnv *)SDL_AndroidGetJNIEnv();
	jobject activity = SDL_AndroidGetActivity();
	jclass activityClass;
	jmethodID setVisibility;

	if (!env || !activity) {
		fprintf(stderr, "Could not access the Android activity for DLC button visibility\n");
		return;
	}

	activityClass = (*env)->GetObjectClass(env, activity);
	if (!activityClass) {
		fprintf(stderr, "Could not resolve the Android activity class\n");
		(*env)->DeleteLocalRef(env, activity);
		return;
	}

	setVisibility = (*env)->GetMethodID(env, activityClass,
			"setDlcButtonVisible", "(Z)V");
	if (!setVisibility) {
		fprintf(stderr, "Could not resolve setDlcButtonVisible on the Android activity\n");
		if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
		(*env)->DeleteLocalRef(env, activityClass);
		(*env)->DeleteLocalRef(env, activity);
		return;
	}

	(*env)->CallVoidMethod(env, activity, setVisibility,
			visible ? JNI_TRUE : JNI_FALSE);
	if ((*env)->ExceptionCheck(env)) {
		fprintf(stderr, "Could not update Android DLC button visibility\n");
		(*env)->ExceptionDescribe(env);
		(*env)->ExceptionClear(env);
	}

	(*env)->DeleteLocalRef(env, activityClass);
	(*env)->DeleteLocalRef(env, activity);
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
