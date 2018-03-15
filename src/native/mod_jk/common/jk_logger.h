/*
 * Copyright (c) 1997-1999 The Java Apache Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software developed by the Java Apache 
 *    Project for use in the Apache JServ servlet engine project
 *    <http://java.apache.org/>."
 *
 * 4. The names "Apache JServ", "Apache JServ Servlet Engine" and 
 *    "Java Apache Project" must not be used to endorse or promote products 
 *    derived from this software without prior written permission.
 *
 * 5. Products derived from this software may not be called "Apache JServ"
 *    nor may "Apache" nor "Apache JServ" appear in their names without 
 *    prior written permission of the Java Apache Project.
 *
 * 6. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software developed by the Java Apache 
 *    Project for use in the Apache JServ servlet engine project
 *    <http://java.apache.org/>."
 *    
 * THIS SOFTWARE IS PROVIDED BY THE JAVA APACHE PROJECT "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JAVA APACHE PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Java Apache Group. For more information
 * on the Java Apache Project and the Apache JServ Servlet Engine project,
 * please see <http://java.apache.org/>.
 *
 */

/***************************************************************************
 * Description: Logger object definitions                                  *
 * Author:      Gal Shachor <shachor@il.ibm.com>                           *
 * Version:     $Revision: 1.1 $                                               *
 ***************************************************************************/

#ifndef JK_LOGGER_H
#define JK_LOGGER_H

#include "jk_global.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

typedef struct jk_logger jk_logger_t;
struct jk_logger {
    void *logger_private;
    int  level;

    int (JK_METHOD *log)(jk_logger_t *l,
                         int level,
                         const char *what);

};

#define JK_LOG_DEBUG_LEVEL 0
#define JK_LOG_INFO_LEVEL  1
#define JK_LOG_ERROR_LEVEL 2
#define JK_LOG_EMERG_LEVEL 3

#define JK_LOG_DEBUG_VERB   "debug"
#define JK_LOG_INFO_VERB    "info"
#define JK_LOG_ERROR_VERB   "error"
#define JK_LOG_EMERG_VERB   "emerg"

#define JK_LOG_DEBUG __FILE__,__LINE__,JK_LOG_DEBUG_LEVEL
#define JK_LOG_INFO  __FILE__,__LINE__,JK_LOG_INFO_LEVEL
#define JK_LOG_ERROR __FILE__,__LINE__,JK_LOG_ERROR_LEVEL
#define JK_LOG_EMERG __FILE__,__LINE__,JK_LOG_EMERG_LEVEL

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* JK_LOGGER_H */
