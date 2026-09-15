export const environment = {
    production: false,

    nodes: [
        {id:'node-a', baseUrl: 'http://localhost:8081', color: '#00e5ff' },
        {id:'node-b', baseUrl: 'http://localhost:8082', color: '#aa55ff' },
        {id:'node-c', baseUrl: 'http://localhost:8083', color: '#ff4466' },
        {id:'node-d', baseUrl: 'http://localhost:8084', color: '#ffcc00' },
        {id:'node-e', baseUrl: 'http://localhost:8085', color: '#00ff88' },
      ],

    primaryNode: 'http://localhost:8081',

    pollIntervals: {
        nodes: 3_000, distribution: 5_000, hotKeys: 10_000
      },

    sseReconnectDelayMs: 3_000,

    hotKeyThreshold: 2.0,

  } as const;
