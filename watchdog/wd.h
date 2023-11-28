#ifndef __ILRD_139_40__WD_H__
#define __ILRD_139_40__WD_H__

#include <stddef.h> /* size_t */

/****************************************************************************
* Watchdog Description:														*
*	Watchdog insures your program will finish running the code between the	*
*	StartWD and StopWD functions by restarting your program on termination.	*
*	The program uses the given threshold to count *2* seconds intervals, 	*
*	for example, threshold of 6 will cause the program to be restarted		*
*	after 12 seconds when being unresponsive.								*
*	Can be included with the 'libwd.so' library.							*
*	Main function must take argc and argv parameters (can be empty).		*
*	Thread safety: MT-Unsafe!												*
****************************************************************************/

/*
* StartWD Description:
*	Starts the Watchdog.
*
* @Params:
*   argc - same argc recieved by the main function.
*   argv - same argv recieved by the main function.
*	threshold - number of miss signals to restart user process.
*
* @Returns:
*	Return 0 in success, otherwise a non-zero value.
*
* @Complexity
*	Time: O(n)
*/
int StartWD(int argc, char *argv[], size_t threshold);

/*
* StopWD Description:
*	stops the Watchdog
*
* @Params:
*	None.
*
* @Returns:
*	None.
*
* @Complexity
*	Time: O(1)
*/
void StopWD();

#endif