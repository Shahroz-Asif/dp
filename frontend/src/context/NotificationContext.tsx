import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import type { ReactNode } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { notificationRepository } from '../api/notifications';
import type { NotificationResponse } from '../types/api';

interface NotificationContextType {
  notifications: NotificationResponse[];
  unreadCount: number;
  markRead: (id: number) => Promise<void>;
  markAllRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextType | null>(null);

interface Props {
  children: ReactNode;
  /** Pass null when not logged in — context will stay disconnected. */
  credentials: { username: string; password: string } | null;
}

/**
 * Provides real-time (WebSocket/STOMP) and persisted notification state.
 *
 * On mount (with valid credentials):
 *  1. Fetches all persisted notifications via REST so offline-received events
 *     are immediately visible.
 *  2. Opens a STOMP connection over SockJS, subscribing to
 *     /user/queue/notifications for live pushes.
 *
 * Cleans up the STOMP connection on logout (credentials → null).
 */
export function NotificationProvider({ children, credentials }: Props) {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const stompRef = useRef<Client | null>(null);

  const unreadCount = notifications.filter((n) => !n.read).length;

  // -------------------------------------------------------------------------
  // Load persisted notifications + connect to WebSocket
  // -------------------------------------------------------------------------
  useEffect(() => {
    if (!credentials) {
      // Logged out — clear state and disconnect
      setNotifications([]);
      stompRef.current?.deactivate();
      stompRef.current = null;
      return;
    }

    // 1. Fetch persisted notifications (for offline-while-away messages)
    notificationRepository.getAll().then(setNotifications).catch(() => {});

    // 2. Connect STOMP over SockJS
    const client = new Client({
      // SockJS factory keeps the Vite proxy path consistent with HTTP
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        login: credentials.username,
        passcode: credentials.password,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (frame) => {
          const notification: NotificationResponse = JSON.parse(frame.body);
          setNotifications((prev) => [notification, ...prev]);
        });
      },
    });

    client.activate();
    stompRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [credentials?.username, credentials?.password]);

  // -------------------------------------------------------------------------
  // Actions
  // -------------------------------------------------------------------------
  const markRead = useCallback(async (id: number) => {
    const updated = await notificationRepository.markRead(id);
    setNotifications((prev) => prev.map((n) => (n.id === id ? updated : n)));
  }, []);

  const markAllRead = useCallback(async () => {
    await notificationRepository.markAllRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  }, []);

  return (
    <NotificationContext.Provider value={{ notifications, unreadCount, markRead, markAllRead }}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications(): NotificationContextType {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error('useNotifications must be used within <NotificationProvider>');
  return ctx;
}
