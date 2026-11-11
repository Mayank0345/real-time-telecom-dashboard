const fromInput = document.querySelector("#from-date");
const toInput = document.querySelector("#to-date");
const regionFilter = document.querySelector("#region-filter");
const errorMessage = document.querySelector("#error-message");
const apiBaseUrl = (window.TELECOM_API_BASE_URL || "").replace(/\/+$/, "");

const today = new Date();
const startDate = new Date(today);
startDate.setDate(today.getDate() - 6);
fromInput.value = toInputValue(startDate);
toInput.value = toInputValue(today);

let usageChart;
let regionChart;
let monthlyChart;

function toInputValue(date) {
    const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return localDate.toISOString().slice(0, 10);
}

async function loadRegions() {
    const regions = await fetchJson("/api/regions");
    for (const region of regions) {
        const option = document.createElement("option");
        option.value = region.id;
        option.textContent = region.name;
        regionFilter.append(option);
    }
}

async function loadDashboard() {
    errorMessage.hidden = true;
    const params = new URLSearchParams({ from: fromInput.value, to: toInput.value });
    if (regionFilter.value) params.set("regionId", regionFilter.value);
    const query = params.toString();

    try {
        const [stats, usageTrend, regionalUsage, outages, monthlyUsage, recentUsage] = await Promise.all([
            fetchJson(`/api/dashboard/stats?${query}`),
            fetchJson(`/api/usage/daily?${query}`),
            fetchJson(`/api/usage/region?${query}`),
            fetchJson(`/api/outages?${query}`),
            fetchJson(`/api/usage/monthly${regionFilter.value ? `?regionId=${encodeURIComponent(regionFilter.value)}` : ""}`),
            fetchJson(`/api/usage/recent?${query}`)
        ]);
        renderDashboard({ stats, usageTrend, regionalUsage, outages, monthlyUsage, recentUsage });
        document.querySelector("#last-updated").textContent =
            `Updated ${new Date().toLocaleTimeString()}`;
    } catch (error) {
        errorMessage.textContent = error.message || "Unable to connect to the dashboard API.";
        errorMessage.hidden = false;
    }
}

async function fetchJson(url) {
    const response = await fetch(`${apiBaseUrl}${url}`);
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.error || `Request failed (${response.status}).`);
    }
    return data;
}

function renderDashboard(data) {
    document.querySelector("#subscriber-count").textContent =
        data.stats.totalSubscribers.toLocaleString();
    document.querySelector("#active-subscriber-count").textContent =
        data.stats.activeSubscribers.toLocaleString();
    document.querySelector("#inactive-subscriber-count").textContent =
        (data.stats.totalSubscribers - data.stats.activeSubscribers).toLocaleString();
    document.querySelector("#usage-total").textContent =
        `${(data.stats.totalDataUsage / 1024).toLocaleString(undefined, { maximumFractionDigits: 1 })} GB`;
    document.querySelector("#average-call-duration").textContent =
        `${data.stats.averageCallDuration.toLocaleString(undefined, { maximumFractionDigits: 1 })} min`;
    document.querySelector("#sms-total").textContent = data.stats.totalSmsCount.toLocaleString();
    document.querySelector("#outage-count").textContent = data.stats.activeOutages.toLocaleString();
    renderUsageChart(data.usageTrend);
    renderRegionChart(data.regionalUsage);
    renderMonthlyChart(data.monthlyUsage);
    renderRegionalStatus(data.regionalUsage);
    renderRecentUsage(data.recentUsage);
    renderOutages(data.outages);
}

function renderUsageChart(points) {
    const context = document.querySelector("#usage-chart");
    if (usageChart) usageChart.destroy();
    usageChart = new Chart(context, {
        type: "line",
        data: {
            labels: points.map(point => new Date(`${point.date}T00:00:00`).toLocaleDateString(
                undefined, { month: "short", day: "numeric" })),
            datasets: [{
                data: points.map(point => point.usageMb),
                borderColor: "#2878df",
                backgroundColor: "rgba(40, 120, 223, .12)",
                borderWidth: 2,
                fill: true,
                tension: .35,
                pointRadius: 3,
                pointBackgroundColor: "#2878df"
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { display: false } },
                y: { beginAtZero: true, grid: { color: "#edf1f6" } }
            }
        }
    });
}

function renderRegionChart(regions) {
    const context = document.querySelector("#region-chart");
    if (regionChart) regionChart.destroy();
    regionChart = new Chart(context, {
        type: "doughnut",
        data: {
            labels: regions.map(region => region.regionName),
            datasets: [{
                data: regions.map(region => region.usageMb),
                backgroundColor: ["#2878df", "#18a77b", "#f3a544", "#8865d5"],
                borderWidth: 0,
                hoverOffset: 5
            }]
        },
        options: {
            maintainAspectRatio: false,
            cutout: "67%",
            plugins: { legend: { position: "bottom", labels: { usePointStyle: true, padding: 18 } } }
        }
    });
}

function renderMonthlyChart(points) {
    const context = document.querySelector("#monthly-chart");
    if (monthlyChart) monthlyChart.destroy();
    monthlyChart = new Chart(context, {
        type: "bar",
        data: {
            labels: points.map(point => new Date(`${point.month}-01T00:00:00`).toLocaleDateString(
                undefined, { month: "short", year: "2-digit" })),
            datasets: [{
                data: points.map(point => point.usageMb / 1024),
                backgroundColor: "rgba(40, 120, 223, .78)",
                borderRadius: 5,
                maxBarThickness: 38
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { display: false } },
                y: { beginAtZero: true, title: { display: true, text: "GB" },
                    grid: { color: "#edf1f6" } }
            }
        }
    });
}

function renderRegionalStatus(regions) {
    const rows = document.querySelector("#region-rows");
    rows.replaceChildren();

    if (regions.length === 0) {
        const row = rows.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 3;
        cell.className = "empty-row";
        cell.textContent = "No regions available.";
        return;
    }

    for (const region of regions) {
        const row = rows.insertRow();
        row.insertCell().textContent = region.regionName;
        row.insertCell().textContent =
            `${(region.usageMb / 1024).toLocaleString(undefined, { maximumFractionDigits: 1 })} GB`;
        const statusCell = row.insertCell();
        const status = document.createElement("span");
        const hasOutage = region.activeOutages > 0;
        status.className = `status ${hasOutage ? "active" : "normal"}`;
        status.textContent = hasOutage
            ? `Outage${region.activeOutages > 1 ? ` (${region.activeOutages})` : ""}`
            : "Normal";
        statusCell.append(status);
    }
}

function renderRecentUsage(records) {
    const rows = document.querySelector("#recent-usage-rows");
    rows.replaceChildren();

    if (records.length === 0) {
        const row = rows.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 6;
        cell.className = "empty-row";
        cell.textContent = "No usage records for this period.";
        return;
    }

    for (const record of records) {
        const row = rows.insertRow();
        row.insertCell().textContent = new Date(record.timestamp).toLocaleString();
        row.insertCell().textContent = record.subscriberName;
        row.insertCell().textContent = record.regionName;
        row.insertCell().textContent = `${record.dataUsage.toLocaleString()} MB`;
        row.insertCell().textContent = `${record.callDuration} min`;
        row.insertCell().textContent = record.smsCount.toLocaleString();
    }
}

function renderOutages(outages) {
    const rows = document.querySelector("#outage-rows");
    rows.replaceChildren();
    document.querySelector("#incident-count").textContent =
        `${outages.length} incident${outages.length === 1 ? "" : "s"}`;

    if (outages.length === 0) {
        const row = rows.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 4;
        cell.className = "empty-row";
        cell.textContent = "No outages recorded for this period.";
        return;
    }

    for (const outage of outages) {
        const row = rows.insertRow();
        row.insertCell().textContent = outage.regionName;
        row.insertCell().textContent = outage.reason;
        row.insertCell().textContent = new Date(outage.startTime).toLocaleString();
        const statusCell = row.insertCell();
        const status = document.createElement("span");
        const isActive = outage.endTime === null;
        status.className = `status ${isActive ? "active" : "resolved"}`;
        status.textContent = isActive ? "Active" : "Resolved";
        statusCell.append(status);
    }
}

document.querySelector("#apply-filters").addEventListener("click", loadDashboard);

async function startDashboard() {
    try {
        await loadRegions();
        await loadDashboard();
        window.setInterval(loadDashboard, 10_000);
    } catch (error) {
        errorMessage.textContent = error.message || "Unable to start the dashboard.";
        errorMessage.hidden = false;
    }
}

startDashboard();
