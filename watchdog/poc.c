#include <stdio.h> /* perror */
#include <unistd.h> /* fork */
#include <sys/types.h> /* pid */
#include <sys/wait.h> /* wait */
#include <signal.h> /* SIG... */
#include <stdlib.h> /* atoi */
#include <semaphore.h> /* sem_t */
#include <fcntl.h> /* For O_* constants */
#include <string.h> /* strdup */
#include <stdlib.h> /* setenv */

#include "sched.h"
#include "wd.h"

#define MAX_TH_LEN (5)

enum ret_vals {FAILURE = -1, SUCCESS = 0, WD_FAIL = 1, USER_FAIL = 2};

/******************** GLOBAL VARIABLES ******************/

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
sig_atomic_t counter = 0;
sig_atomic_t send_pid = 0;
pthread_t tid = 0;
size_t threshold = 0;
sem_t *sem = NULL;
char **custom_argv = NULL;
sch_t *sch = NULL;

/***************** FUNCTION DECLARATIONS ****************/

void USR1Handler(int sig, siginfo_t *info, void *context);
void USR2Handler(int sig, siginfo_t *info, void *context);

static int CreateWD(void);
void *ThreadCom(void *arg);

static int SendSignal(void *param);
static int CheckCounter(void *param);

/******************* API FUNCTIONS *********************/

int StartWD(int argc, char *argv[], size_t th)
{
	char th_buf[MAX_TH_LEN] = {0};
	struct sigaction sa[2] = {0};

	threshold = th;
	custom_argv = argv;

	sch = SchCreate();

	sa[0].sa_flags = SA_SIGINFO;
	sa[0].sa_sigaction = &USR1Handler;

	sa[1].sa_flags = SA_SIGINFO;
	sa[1].sa_sigaction = &USR2Handler;

	if (-1 == sigaction(SIGUSR2, &sa[1], NULL) || -1 == sigaction(SIGUSR1, &sa[0], NULL))
	{
		perror("sigaction()");
		return FAILURE;
	}

	sem = sem_open("/wd_sem", O_CREAT, 0666, 0);
	if (SEM_FAILED == sem)
	{
		perror("sem_open()");
		return FAILURE;
	}

	SchAddTask(sch, &SendSignal, NULL, (time(NULL) + (time_t) 3), 2);
	SchAddTask(sch, &CheckCounter, NULL, (time(NULL) + (time_t) 4), 4);

	if (0 != strcmp(argv[0], "./wd.out"))
	{
		sprintf(th_buf, "%lu", threshold);

		setenv("THRESHOLD", th_buf, 1);
		setenv("USER_EXEC", argv[0], 1);

		custom_argv[0] = strdup("./wd.out");

		if (0 != pthread_create(&tid, NULL, &ThreadCom, NULL))
		{
			perror("pthread_create()");
			return FAILURE;
		}

		CreateWD();
	}
	else
	{
		custom_argv[0] = strdup(getenv("USER_EXEC"));

		send_pid = getppid();
		kill(send_pid, SIGCONT);
		sem_post(sem);

		SchRun(sch);

		SchDestroy(sch);
	}

	return SUCCESS;
}


void StopWD()
{
	kill(send_pid, SIGUSR2);
	raise(SIGUSR2);
	wait(NULL);
	pthread_join(tid, NULL);
	sem_unlink("/wd_sem");
}

/******************** THREAD FUNCTIONS *******************/

void *ThreadCom(void *arg)
{
	int ret = SUCCESS;

	(void) arg;

	sem_wait(sem);
	ret = SchRun(sch);
	SchDestroy(sch);

	if (WD_FAIL == ret)
	{
		execvp(custom_argv[0], custom_argv);
		perror("execvp()");
	}

	return NULL;
}

/****************** SCHEDULER TASK FUNCTIONS ***************/

static int SendSignal(void *param)
{
	(void) param;

	kill(send_pid, SIGUSR1);
	pthread_mutex_lock(&mutex);
	++counter;
	pthread_mutex_unlock(&mutex);
	
	return SUCCESS;
}

static int CheckCounter(void *param)
{
	(void) param;

	#ifndef NDEBUG
	printf("counter is: %d in %s\n", counter, custom_argv[0]);
	#endif
	pthread_mutex_lock(&mutex);
	if ((sig_atomic_t)threshold < counter)
	{
		if (0 == strcmp(custom_argv[0], "./wd.out"))
		{
			wait(NULL);
			CreateWD();
			counter = 0;
		}
		else
		{
			raise(SIGUSR2);
			return WD_FAIL;
		}
	}
	pthread_mutex_unlock(&mutex);
	
	return SUCCESS;
}

/******************** SIGNAL HANDLERS ********************/

void USR1Handler(int sig, siginfo_t *info, void *context)
{
	(void) sig;
	(void) info;
	(void) context;

	pthread_mutex_lock(&mutex);
	counter = 0;
	pthread_mutex_unlock(&mutex);
}

void USR2Handler(int sig, siginfo_t *info, void *context)
{
	(void) sig;
	(void) info;
	(void) context;

	sem_close(sem);
	SchStop(sch);
	free(custom_argv[0]);
}

/******************* STATIC FUNCTIONS *****************/

static int CreateWD(void)
{
	send_pid = fork();

	if (-1 == send_pid)
	{
		perror("Fork:");
		return FAILURE;
	}
	else if (0 == send_pid)
	{
		execvp(custom_argv[0], custom_argv);
		perror("execvp()");
		return FAILURE;
	}

	return SUCCESS;
}