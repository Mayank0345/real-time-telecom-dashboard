const localHosts = ["localhost", "127.0.0.1"];
window.TELECOM_API_BASE_URL = localHosts.includes(window.location.hostname)
    ? ""
    : "https://real-time-telecom-dashboard.onrender.com";
