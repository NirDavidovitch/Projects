#include <signal.h> /* SIG... */
#include <sys/types.h> /* pid */
#include <stdio.h> /* printf */
#include <stdlib.h> /* atoi */
#include <unistd.h> /* sleep */
#include <fcntl.h> /* For O_* constants */
#include <semaphore.h> /* sem_t */

#include "wd.h"

int main(int argc, char *argv[])
{
	size_t threshold = 0;

	#ifndef NDEBUG
	printf("WD started %s\n", argv[1]);
	#endif

	threshold = atoi(getenv("THRESHOLD"));
	if (0 == threshold)
	{
		perror("atoi()");
		return 1;
	}
	StartWD(argc, argv, threshold);
	
	return 0;
}