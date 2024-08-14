package org.simplejavamail.mailer.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simplejavamail.api.internal.authenticatedsockssupport.socks5server.AnonymousSocks5Server;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Starts and stops the authenticated proxy server when needed.
 * <p>
 * Note that this Runnable implementation is <strong>not</strong> thread related, it is just to encapsulate the code to
 * be run directly or from a <em>real</em> Runnable.
 */
public abstract class AbstractProxyServerSyncingClosure implements Runnable {

	protected static final Logger LOGGER = getLogger(AbstractProxyServerSyncingClosure.class);

	@NotNull private final AtomicInteger smtpConnectionCounter;
	@Nullable private final AnonymousSocks5Server proxyServer;

	AbstractProxyServerSyncingClosure(@NotNull final AtomicInteger smtpConnectionCounter, @Nullable final AnonymousSocks5Server proxyServer) {
		this.smtpConnectionCounter = smtpConnectionCounter;
		this.proxyServer = proxyServer;

		increaseSmtpConnectionCounter();
	}

	private void increaseSmtpConnectionCounter() {
		synchronized (smtpConnectionCounter) {
			smtpConnectionCounter.incrementAndGet();
		}
	}

	@Override
	public final void run() {
		try {
			startProxyServerIfNeeded();
			executeClosure();
		} finally {
			shutDownProxyServerIfRunningAndCurrentBatchCompleted();
		}
	}

	abstract void executeClosure();

	private void startProxyServerIfNeeded() {
		if (proxyServer != null) {
			synchronized (proxyServer) {
				if (!proxyServer.isRunning()) {
					LOGGER.trace("starting proxy bridge...");
					proxyServer.start();
				}
			}
		}
	}

	private void shutDownProxyServerIfRunningAndCurrentBatchCompleted() {
		synchronized (smtpConnectionCounter) {
			if (smtpConnectionCounter.decrementAndGet() == 0) {
				LOGGER.trace("all threads have finished processing");
				if (proxyServer != null) {
					synchronized (proxyServer) {
						if (proxyServer.isRunning() && !proxyServer.isStopping()) {
							LOGGER.trace("stopping proxy bridge...");
							proxyServer.stop();
						}
					}
				}
			} else {
				LOGGER.trace("SMTP request threads left: {}", smtpConnectionCounter.get());
			}
		}
	}
}
