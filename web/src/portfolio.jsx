import { useState, useEffect } from "react";
import Box from "@mui/material/Box";
import Loader from "./Loader";
import DataGrid from "./datagrid";
import Api from "./api";
import Util from "./util";

const fields = [
  { field: "symbol", label: "Symbol", sortable: true },
  { field: "name", label: "Name", sortable: true },
  { field: "position", label: "Position", sortable: false, decimals: 0 },
  { field: "price", label: "Price", sortable: false, decimals: 3 },
  { field: "dividends", label: "Dividends", sortable: true, colour: true, decimals: 2 },
  { field: "changeToday", label: "Today P&L", sortable: true, colour: true, decimals: 2 },
  { field: "changeTodayPercentage", label: "Today %", sortable: true, colour: true, decimals: 1 },
  { field: "profit", label: "P&L", sortable: true, colour: true, decimals: 2 },
  { field: "profitPercentage", label: "P&L %", sortable: true, colour: true, decimals: 1 },
];

const Portfolio = () => {
  const [ data, setData ] = useState();
  const { getPortfolio } = Api();
  const { sort } = Util();

  const defaultSortField = "changeTodayPercentage";
  const filterData = (data, criteria) => sort(data.filter(criteria), defaultSortField, false);

  useEffect(() => getPortfolio((response) => {
    const sg = filterData(response, (i) => i.symbol.endsWith(":xses"));
    const us = filterData(response, (i) => i.symbol.indexOf(":") > -1 && !i.symbol.endsWith(":xses"));
    const crypto = filterData(response, (i) => i.symbol.indexOf(":") === -1);
    setData({ sg, us, crypto });
  }), []);

  const getSummary = (data) => {
    const amount = data.map((i) => i.amount).reduce((a, b) => (a += b));
    const changeToday = data
      .map((i) => i["changeToday"])
      .reduce((a, b) => (a += b));
    const changeTodayPercentage = (changeToday * 100) / amount;
    const profit = data.map((i) => i["profit"]).reduce((a, b) => (a += b));
    const profitPercentage = (profit * 100) / amount;

    return {
      skip: 5,
      sFields: [
        { value: changeToday, ...fields[5] },
        { value: changeTodayPercentage, ...fields[6] },
        { value: profit, ...fields[7] },
        { value: profitPercentage, ...fields[8] },
      ]
    };
  };

  const toTitleCase = (input) => input.replace(/\w\S*/g, (txt) => txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase());
  const toProperCase = (input) => input.length <= 2 ? input.toUpperCase() : toTitleCase(input);

  return !data ? <Loader /> : (
    <Box>
      { Object.keys(data).map((k) => data[k].length > 0 &&
        <DataGrid
          key={k}
          label={`${toProperCase(k)} Portfolio`}
          data={data[k]}
          summary={getSummary(data[k])}
          {...{ fields, defaultSortField }}
        />)
      }
    </Box>
  );
};

export default Portfolio;
