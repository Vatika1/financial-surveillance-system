import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL;

export const options = {
    scenarios: {
        trades: {
            executor: 'constant-arrival-rate',
            rate: __ENV.RATE ? parseInt(__ENV.RATE) : 200,
            timeUnit: '1s',
            duration: __ENV.DURATION || '15m',
            preAllocatedVUs: 50,
            maxVUs: 400,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

const SYMBOLS = ['AAPL', 'MSFT', 'GOOG', 'AMZN', 'TSLA', 'NVDA'];

export default function () {
    const now = Date.now();
    const advisor = `ADV-${(__VU % 50).toString().padStart(3, '0')}`;

    const payload = JSON.stringify({
        tradeId: `TRD-K6-${__VU}-${__ITER}-${now}`,
        advisorId: advisor,
        accountId: `ACC-${__VU % 200}`,
        clientId: `CLI-${__VU % 200}`,
        symbol: SYMBOLS[__ITER % SYMBOLS.length],
        tradeType: __ITER % 2 === 0 ? 'BUY' : 'SELL',
        quantity: 100 + (__ITER % 900),
        price: 150.0,
        currency: 'USD',
        exchange: 'NYSE',
        tradeTimestamp: new Date(now).toISOString(),
        sourceSystem: 'ETRADE',
        sourceSystemId: `SRC-${__ITER}`,
    });

    const res = http.post(`${BASE_URL}/api/trades`, payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, { 'status is 201': (r) => r.status === 201 });
    if (res.status !== 201) console.log(res.status, res.body);
}