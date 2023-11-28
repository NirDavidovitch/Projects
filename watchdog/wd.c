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
#include <pthread.h> /* pthread, mutex */

#include "sched.h"
#include "wd.h"

/************************* MACROS ***********************/

#define MAX_TH_LEN (5)
#define WD_EXEC ("./wdmain.out")

/**************************** ENUMS *********************/

enum ret_vals {FAILURE = -1, SUCCESS = 0, WD_FAIL = 1, USER_FAIL = 2, STOP_SCH = 3};
enum bool {FALSE = 0, TRUE = 1};

/******************** GLOBAL VARIABLES ******************/

_Atomic int stop_flag = FALSE;
_Atomic size_t counter = 0;
_Atomic pid_t send_pid = 0;
pthread_t user_tid = 0;
size_t threshold = 0;
sem_t *sem = NULL;
sem_t *sync_sem = NULL;
char **custom_argv = NULL;
sch_t *sch = NULL;

/***************** FUNCTION DECLARATIONS ****************/

void USR1Handler(int sig, siginfo_t *info, void *context);
void USR2Handler(int sig, siginfo_t *info, void *context);

static int CreateWD(void);
static void CleanUp(void);
void *ThreadCom(void *arg);

static int SendSignal(void *param);
static int CheckCounter(void *param);

/******************* API FUNCTIONS *********************/

int StartWD(int argc, char *argv[], size_t th)
{
	char th_buf[MAX_TH_LEN] = {0};
	struct sigaction sa[2] = {0};
	int ret = SUCCESS;

	(void) argc;

	threshold = th;
	custom_argv = argv;
	/* can fail */
	setenv("WD_EXEC", WD_EXEC, 1);

	sch = SchCreate();

	sa[0].sa_sigaction = &USR1Handler;
	sa[1].sa_sigaction = &USR2Handler;

	if (-1 == sigaction(SIGUSR2, &sa[1], NULL) || -1 == sigaction(SIGUSR1, &sa[0], NULL))
	{
		perror("sigaction()");
		unsetenv("WD_EXEC");
		SchDestroy(sch);
		return FAILURE;
	}

	sem = sem_open("/wd_sem", O_CREAT, 0666, 0);
	if (SEM_FAILED == sem)
	{
		perror("sem_open()");
		unsetenv("WD_EXEC");
		SchDestroy(sch);
		return FAILURE;
	}

	sync_sem = sem_open("/wd_sync_sem", O_CREAT, 0666, 0);
    if (SEM_FAILED == sync_sem)
    {
        perror("sem_open()");
		unsetenv("WD_EXEC");
		sem_close(sem);
		sem_unlink("/wd_sem");
		SchDestroy(sch);
        return FAILURE;
    }

	/* can fail */
	SchAddTask(sch, &SendSignal, NULL, (time(NULL) + (time_t) 3), 2);
	/* can fail */
	SchAddTask(sch, &CheckCounter, NULL, (time(NULL) + (time_t) 4), 4);

	if (0 != strcmp(argv[0], WD_EXEC))
	{
		sprintf(th_buf, "%lu", threshold);

		/* can fail */
		setenv("THRESHOLD", th_buf, 1);
		/* can fail */
		setenv("USER_EXEC", argv[0], 1);

		custom_argv[0] = getenv("WD_EXEC");

		if (0 != pthread_create(&user_tid, NULL, &ThreadCom, NULL))
		{
			perror("pthread_create()");
			CleanUp();
			sem_unlink("/wd_sem");
			sem_unlink("/wd_sync_sem");
			SchDestroy(sch);
			return FAILURE;
		}
		
		/* can fail */
		CreateWD();
	}
	else
	{
		custom_argv[0] = getenv("USER_EXEC");

		send_pid = getppid();
		kill(send_pid, SIGCONT);
		
		sem_post(sem);
		sem_wait(sync_sem);

		ret = SchRun(sch);
		SchDestroy(sch);

		if (USER_FAIL == ret)
		{
			execvp(custom_argv[0], custom_argv);
			perror("execvp()");
			return FAILURE;
		}
	}

	return SUCCESS;
}

void StopWD()
{
	kill(send_pid, SIGUSR2);
	raise(SIGUSR2);
	wait(NULL);
	pthread_join(user_tid, NULL);
	sem_unlink("/wd_sem");
	sem_unlink("/wd_sync_sem");
}

/******************** THREAD FUNCTIONS *******************/

void *ThreadCom(void *arg)
{
	(void) arg;

	sem_post(sync_sem);
	sem_wait(sem);
	SchRun(sch);

	SchDestroy(sch);

	return NULL;
}

/****************** SCHEDULER TASK FUNCTIONS ***************/

static int SendSignal(void *param)
{
	(void) param;

	kill(send_pid, SIGUSR1);
	++counter;
	
	return SUCCESS;
}

static int CheckCounter(void *param)
{
	(void) param;

	if (stop_flag)
	{
		return STOP_SCH;
	}

	#ifndef NDEBUG
	printf("waiting %d intervals for %s\n", counter, custom_argv[0]);
	#endif
	if (threshold < counter)
	{
		if (0 == strcmp(custom_argv[0], WD_EXEC))
		{
			wait(NULL);
			if (SUCCESS != CreateWD())
			{
				return WD_FAIL;
			}
			counter = 0;
		}
		else
		{
			return USER_FAIL;
		}
	}

	return SUCCESS;
}

/******************** SIGNAL HANDLERS ********************/

void USR1Handler(int sig, siginfo_t *info, void *context)
{
	(void) sig;
	(void) info;
	(void) context;

	counter = 0;
}

void USR2Handler(int sig, siginfo_t *info, void *context)
{
	(void) sig;
	(void) info;
	(void) context;

	CleanUp();
	stop_flag = TRUE;
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

static void CleanUp(void)
{
	sem_close(sem);
	sem_close(sync_sem);

	unsetenv("WD_EXEC");
	unsetenv("THRESHOLD");
	unsetenv("USER_EXEC");
}