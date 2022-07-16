package org.dvlyyon.net.gnmi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dvlyyon.common.util.RFC3399Timestamp;

import gnmi.Gnmi.SubscribeRequest;
import gnmi.Gnmi.SubscribeResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public abstract class GnmiCommonClient 
implements GnmiClientInf, GnmiClientInternalInf {

	private static final Logger logger = 
			Logger.getLogger(GnmiCommonClient.class.getName());
	protected ManagedChannel 		channel;
	protected GnmiClientContextInf 	context;

	public void close() throws IOException {
		if (channel == null) return;
		try {
			channel.shutdownNow();
			channel.awaitTermination(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception when shutdown channel", e);
			throw new IOException (e.getMessage());
		}
	}
	
	public boolean isConnected() {
		if (channel == null) return false;
		return channel.isShutdown();
	}
	
	protected static class DefaultSubscriptionMgr 
	implements SubscriptionInf {
		private static final int DEFAULT_MAX_CAPACITY = 10000;
		private GnmiClientInternalInf 		client;

		GnmiResponse <SubscribeResponse> 		myResponseObserver;
		StreamObserver<SubscribeRequest> 		myRequestObserver;
		ArrayBlockingQueue<Event<SubscribeResponse>>	myQueue;
		
		boolean isComplete = false;
		boolean isError = false;
		boolean ignore	= false;
		String	errorInfo = null;
		long	sendTime  = 0;

		public DefaultSubscriptionMgr(GnmiClientInternalInf client) {
			this(client, DEFAULT_MAX_CAPACITY, null);
		}

		public DefaultSubscriptionMgr(GnmiClientInternalInf client, 
				int capacity,
				GnmiCredentialContextInf ccontext) {
			this.client = client;
			myQueue = new ArrayBlockingQueue<Event<SubscribeResponse>>(capacity);			 
			myResponseObserver = new GnmiResponse<SubscribeResponse>();
			myRequestObserver = client.subscribe(myResponseObserver,ccontext);
			new Thread(new Runnable () {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while (	!myResponseObserver.isCompleted() && 
							!myResponseObserver.isError()) {
						Event<SubscribeResponse> 
						response = myResponseObserver.getMyValue();
						if (response == null || ignore) {
							continue;
						}
						try {
							myQueue.put(response);
						} catch (Exception e) {
							logger.log(Level.SEVERE, "Interrupted when put response", e);
						}
					}
					if (myResponseObserver.isCompleted()) {
						isComplete = true;
					}
					if (myResponseObserver.isError()) {
						isError = true;
						errorInfo = myResponseObserver.getError();
					}
				}
			}, "subscription").start();;
	
		}

		public DefaultSubscriptionMgr(GnmiClientInternalInf client, GnmiCredentialContextInf ccontext) {
			this(client, DEFAULT_MAX_CAPACITY, ccontext);
		}

		@Override
		public List<Event<SubscribeResponse>> popAll() {
			ArrayList<Event<SubscribeResponse>> 
			list = new ArrayList<Event<SubscribeResponse>>();
			myQueue.drainTo(list);
			return list;
		}

		@Override
		public void subscribe(SubscribeRequest request) {
			logger.finest(request.toString());
			sendTime = System.currentTimeMillis();
			myRequestObserver.onNext(request);
		}

		@Override
		public void unsubscribe() {
			myRequestObserver.onCompleted();
			myRequestObserver.onError(null);
		}

		@Override
		public boolean isComplete() {
			return isComplete;
		}

		@Override
		public boolean isError() {
			return isError;
		}

		@Override
		public String getErrorInfo() {
			return errorInfo;
		}

		@Override
		public Event<SubscribeResponse> pop() {
			return myQueue.poll();
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return myQueue.size();
		}

		@Override
		public long getSubscriptionTime() {
			return sendTime;
		}

		@Override
		public String getSubscriptionTimeInRFC3399() {
			RFC3399Timestamp rts = new RFC3399Timestamp(sendTime);
			return rts.toString(TimeZone.getDefault());
		}

		@Override
		public void pause() {
			ignore = true;
			logger.warning("Pause is true, the update received at this point will be discarded");
		}

		@Override
		public void resume() {
			ignore = false;
			logger.warning("Resume is true, the update received at this point will be cached");
		}	
		
	}

	protected static class ResonseConsumer implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub

		}

	}
	
	protected static class GnmiResponse <T>implements 
	io.grpc.stub.StreamObserver<T> , Runnable{
		public final int DEFAULT_QUEUE_SIZE = 5000;
		boolean completed = false;
		boolean error = false;
		String  errorInfo = "";
		Queue <GnmiEvent<T>> queue;

		public GnmiResponse() {
			this.queue = new LinkedBlockingQueue<GnmiEvent<T>>();
		}

		@Override
		public void onNext(T value) {
			synchronized (this) {
				try {
					logger.fine("received one:\n"+value);
					GnmiEvent<T> myEvent = new GnmiEvent<T>();
					myEvent.putTime(System.currentTimeMillis());
					myEvent.putEvent(value);
					queue.add(myEvent);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "queue is full", e);
					e.printStackTrace();
				}
				this.notify();
			}
		}

		private void getCause(StringBuilder sb, Throwable t) {
			if (t != null) {
				t.printStackTrace();
				sb.append("\nCaused by:").append(t);
				getCause(sb,t.getCause());
			}
		}

		@Override
		public void onError(Throwable t) {
			logger.log(Level.SEVERE, "Error when calling", t);
			StringBuilder sb = new StringBuilder();
			sb.append(t.toString());
			getCause(sb,t.getCause());
			System.out.println(sb.toString());
			error = true;
			errorInfo = sb.toString();
			synchronized (this) {
				this.notifyAll();
			}
		}

		@Override
		public void onCompleted() {
			completed = true;
			synchronized (this) {
				this.notifyAll();;
			}
		}

		public T getValue() {
			synchronized(this) {
				try {
					if (!completed && !error && queue.isEmpty()) 
						this.wait();

					if (!queue.isEmpty()) return queue.poll().getEvent();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return null;
			}
		}
		
		public Event<T> getMyValue() {
			synchronized(this) {
				try {
					if (!completed && !error && queue.isEmpty()) 
						this.wait();

					if (!queue.isEmpty()) return queue.poll();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return null;
			}			
		}

		public void clear() {
			synchronized (this) {
				queue.clear();;
				completed = error = false;
			}
		}

		public boolean isError() {
			synchronized (this) {
				return error == true;
			}
		}

		public boolean isCompleted() {
			synchronized (this) {
				return completed == true;
			}
		}

		public String getError() {
			return errorInfo;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (!completed) {
				synchronized(this) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						System.out.println("Interrupted!");
					}
				}
			}
		}

	}
	
	static class NoopStreamObserver<V> implements StreamObserver<V> {
		@Override
		public void onNext(V value) {
		}

		@Override
		public void onError(Throwable t) {
		}

		@Override
		public void onCompleted() {
		}
	}

}
