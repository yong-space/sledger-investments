import { useState } from "react";
import Typography from "@mui/material/Typography";
import Box from "@mui/material/Box";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import Util from "./util";

const DataGrid = ({ label, data, fields, summary, defaultSortField }) => {
  const [ viewData, setViewData ] = useState(data);
  const [ sortField, setSortField ] = useState(defaultSortField);
  const [ sortAsc, setSortAsc ] = useState(false);
  const { sort } = Util();

  const sortData = (newSortField) => {
    let order = sortAsc;
    if (newSortField === sortField) {
      order = !order;
      setSortAsc(order);
    } else {
      setSortField(newSortField);
    }
    setViewData(sort([...data], newSortField, order));
  };

  const HeaderTableCell = ({ field, label, sortable }) => {
    return (
      <TableCell
        key={field}
        onClick={() => sortable && sortData(field)}
        className={sortable ? "select" : ""}
      >
        {label}
        {sortField === field && (
          <Box sx={{ float: "right" }}>{sortAsc ? "▲" : "▼"}</Box>
        )}
      </TableCell>
    );
  };

  const formatNumber = (value, decimals) => {
    const parts = value.toFixed(decimals).split(".");
    const whole = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts[1] && decimals > 0 ? `${whole}.${parts[1]}` : whole;
  };

  const DataTableCell = ({ value, decimals, colour }) => {
    const decimalsDefined = parseInt(decimals) % 1 === 0;
    const formatted =
      !decimalsDefined || !value ? value : formatNumber(value, decimals);
    return (
      <TableCell
        align={decimalsDefined ? "right" : "left"}
        className={colour ? (value > 0 ? "green" : value < 0 ? "red" : "") : ""}
      >
        {formatted}
      </TableCell>
    );
  };

  const SummaryRow = ({ skip, sFields }) => (
    <TableRow>
      { skip > 0 && <TableCell colSpan={skip}></TableCell> }
      { sFields.map((field) => <DataTableCell key={field.field} {...field} />)}
    </TableRow>
  );

  return (
    <>
      <Typography variant="h5">{label}</Typography>
      <TableContainer component={Paper} sx={{ my: "1rem" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              {fields.map((field) => (
                <HeaderTableCell key={field.field} {...field} />
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {viewData.map((row) => (
              <TableRow key={row.symbol}>
                {fields.map((field) => (
                  <DataTableCell
                    key={field.field}
                    value={row[field.field]}
                    {...field}
                  />
                ))}
              </TableRow>
            ))}
            { summary && <SummaryRow {...summary } /> }
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};
export default DataGrid;
