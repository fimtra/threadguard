/*
 * Copyright (c) 2016 Ramon Servadei 
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *    
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fimtra.threadguard;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO
 * 
 * @author Ramon Servadei
 */
public class ThreadGuard
{
    final Executor[] executors;

    public ThreadGuard(Executor... executors)
    {
        this.executors = executors;
    }

    void execute(Object context, Runnable runnable)
    {
        this.executors[context.hashCode() % this.executors.length].execute(runnable);
    }

    /**
     * NOTE: the arguments for the target callback interface must be immutable as the point of
     * external callback invoking and actual processing will differ so if the object arguments are
     * changed in the interim, the call will be handling the argument update not the original for
     * the initial call
     * 
     * @param targetCallback
     * @param target
     * @return
     */
    @SuppressWarnings("unchecked")
    <T> T brace(Class<T> targetCallback, final Object target)
    {
        final InvocationHandler handler = new InvocationHandler()
        {
            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable
            {
                if (method.getReturnType().equals(Void.TYPE))
                {
                    execute(method, new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                method.invoke(target, args);
                            }
                            catch (Exception e)
                            {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    });
                    return null;
                }
                else
                {
                    final CountDownLatch latch = new CountDownLatch(1);
                    final AtomicReference<Object> result = new AtomicReference<Object>(null);
                    execute(method, new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                result.set(method.invoke(target, args));
                            }
                            catch (Exception e)
                            {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            latch.countDown();
                        }
                    });
                    latch.await();
                    return result.get();
                }
            }
        };
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[] { targetCallback }, handler);
    }
}
