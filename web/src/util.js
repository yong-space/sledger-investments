const Util = () => ({
    sort: (data, sortField, sortAsc) => {
        data.sort((a, b) => a[sortField] > b[sortField] ? (sortAsc ? 1 : -1) : (sortAsc ? -1 : 1));
        return data;
    },
});
export default Util;
